package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.photo.PhotoCodec;
import dev.eliasnvx.femboymod.photo.PhotoData;
import dev.eliasnvx.femboymod.photo.PhotoIndex;
import dev.eliasnvx.femboymod.photo.PhotoService;
import dev.eliasnvx.femboymod.photo.PhotoStore;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Printed selfies: paper, cooldown, validation and storage (config defaults: 1 paper, 100 ticks). Each test sets the
 * mock player's game mode itself: the default differs between the Fabric and Forge 1.20.1 test servers.
 */
public final class PhotoGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("photo_print_costs_paper", PhotoGameTests::photoPrintCostsPaper),
            new CosmeticGameTests.Entry("photo_needs_paper_and_waits", PhotoGameTests::photoNeedsPaperAndWaits),
            new CosmeticGameTests.Entry("photo_stored_and_loaded", PhotoGameTests::photoStoredAndLoaded),
            new CosmeticGameTests.Entry("photo_folder_stays_capped", PhotoGameTests::photoFolderStaysCapped));

    private static final int PINK = 0xFFF291BE;
    private static final int PAPER = 3;

    private PhotoGameTests() {
    }

    private static byte[] picture() {
        int[] pixels = new int[PhotoCodec.RAW_BYTES];
        Arrays.fill(pixels, PINK);
        return PhotoCodec.encode(pixels);
    }

    private static ItemStack findPhoto(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(FemboyItems.PHOTO.get())) {
                return player.getInventory().getItem(slot);
            }
        }
        return ItemStack.EMPTY;
    }

    public static void photoPrintCostsPaper(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().add(new ItemStack(Items.PAPER, PAPER));
            PhotoService.upload(player, picture());
            GameTestAsserts.assertValueEqual(helper, player.getInventory().countItem(Items.PAPER), PAPER - 1, "paper left");
            ItemStack photo = findPhoto(player);
            helper.assertFalse(photo.isEmpty(), "photo printed");
            PhotoData data = FemboyComponents.PHOTO.get(photo);
            helper.assertTrue(data != null && data.author().equals(player.getGameProfile().getName()), "photo has id and author");
            PhotoIndex index = PhotoIndex.get(player.server);
            helper.assertTrue(index.contains(data.id()) && index.count(player.getUUID()) == 1, "the world's index lists it as theirs");
            PhotoService.forget(player);
        });
    }

    /**
     * No paper, no photo, and nothing even gets decoded (a garbage upload without paper doesn't count as a bad one);
     * a second print waits for the cooldown; an upload that doesn't decode makes the next one wait (anti-spam).
     */
    public static void photoNeedsPaperAndWaits(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            player.setGameMode(GameType.SURVIVAL);
            PhotoService.upload(player, picture());
            helper.assertTrue(findPhoto(player).isEmpty(), "no paper, no photo");
            PhotoService.upload(player, new byte[]{1, 2, 3});
            player.getInventory().add(new ItemStack(Items.PAPER, PAPER));
            PhotoService.upload(player, picture());
            GameTestAsserts.assertValueEqual(helper, player.getInventory().countItem(Items.PAPER), PAPER - 1,
                    "garbage without paper wasn't decoded, so no wait");
            PhotoService.upload(player, picture());
            GameTestAsserts.assertValueEqual(helper, player.getInventory().countItem(Items.PAPER), PAPER - 1,
                    "second print right away waits for the cooldown");
            PhotoService.forget(player);
            PhotoService.upload(player, new byte[]{1, 2, 3});
            GameTestAsserts.assertValueEqual(helper, player.getInventory().countItem(Items.PAPER), PAPER - 1,
                    "broken data is not printed and costs nothing");
            PhotoService.upload(player, picture());
            GameTestAsserts.assertValueEqual(helper, player.getInventory().countItem(Items.PAPER), PAPER - 1,
                    "after a broken upload the next one waits");
            PhotoService.forget(player);
        });
    }

    public static void photoStoredAndLoaded(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            player.setGameMode(GameType.CREATIVE);
            PhotoService.upload(player, picture()); // creative: free
            PhotoData data = FemboyComponents.PHOTO.get(findPhoto(player));
            helper.assertTrue(data != null, "photo printed in creative without paper");
            byte[] stored = PhotoStore.load(player.server, data.id()).join().orElseThrow();
            helper.assertTrue(PhotoCodec.decode(stored).isPresent(), "stored picture decodes");
            helper.assertTrue(PhotoStore.load(player.server, UUID.randomUUID()).join().isEmpty(), "unknown id: nothing");
            PhotoService.forget(player);
        });
    }

    /**
     * Past a cap the least recently seen picture fades: the author's own past the per-player cap, anyone's past the
     * total; looking at a picture keeps it; the order survives a save. A faded picture's file goes away.
     */
    public static void photoFolderStaysCapped(GameTestHelper helper) {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID a1 = UUID.randomUUID(), a2 = UUID.randomUUID(), a3 = UUID.randomUUID();
        UUID b1 = UUID.randomUUID(), b2 = UUID.randomUUID(), b3 = UUID.randomUUID();
        UUID c1 = UUID.randomUUID();
        PhotoIndex index = new PhotoIndex();
        helper.assertTrue(index.add(a1, alice, 2, 4).isEmpty(), "first picture: nothing fades");
        helper.assertTrue(index.add(a2, alice, 2, 4).isEmpty(), "at the cap: nothing fades");
        index.seen(a1);
        GameTestAsserts.assertValueEqual(helper, index.add(a3, alice, 2, 4), List.of(a2), "her least recently seen fades, not the one just looked at");
        GameTestAsserts.assertValueEqual(helper, index.count(alice), 2, "two of hers left");
        index.add(b1, bob, 2, 4);
        index.add(b2, bob, 2, 4);
        GameTestAsserts.assertValueEqual(helper, index.add(b3, bob, 2, 4), List.of(b1), "his own cap takes his oldest, not hers");
        GameTestAsserts.assertValueEqual(helper, index.add(c1, UUID.randomUUID(), 2, 3), List.of(a1, a3), "past the total the oldest of anyone go");
        GameTestAsserts.assertValueEqual(helper, index.size(), 3, "three left");

        Tag saved = PhotoIndex.CODEC.encodeStart(NbtOps.INSTANCE, index).getOrThrow(false, error -> { });
        PhotoIndex loaded = PhotoIndex.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow(false, error -> { });
        GameTestAsserts.assertValueEqual(helper, loaded.count(bob), 2, "authors survive a save");
        GameTestAsserts.assertValueEqual(helper, loaded.add(UUID.randomUUID(), alice, 0, 3), List.of(b2), "and so does the order");

        WearableGameTests.withPlayer(helper, player -> {
            player.setGameMode(GameType.CREATIVE);
            PhotoService.upload(player, picture()); // creative: free
            PhotoData data = FemboyComponents.PHOTO.get(findPhoto(player));
            helper.assertTrue(data != null, "photo printed");
            PhotoStore.delete(player.server, data.id()).join();
            helper.assertTrue(PhotoStore.load(player.server, data.id()).join().isEmpty(), "a faded picture is gone from disk and cache");
            PhotoService.forget(player);
        });
    }
}
