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
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Printed selfies: paper, cooldown, validation and storage (config defaults: 1 paper, 100 ticks). */
public final class PhotoGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("photo_print_costs_paper", PhotoGameTests::photoPrintCostsPaper),
            new CosmeticGameTests.Entry("photo_needs_paper_and_waits", PhotoGameTests::photoNeedsPaperAndWaits),
            new CosmeticGameTests.Entry("photo_stored_and_loaded", PhotoGameTests::photoStoredAndLoaded),
            new CosmeticGameTests.Entry("photo_folder_stays_capped", PhotoGameTests::photoFolderStaysCapped));

    private PhotoGameTests() {
    }

    private static byte[] picture() {
        int[] pixels = new int[PhotoCodec.RAW_BYTES];
        Arrays.fill(pixels, 0xFFF291BE);
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
            player.getInventory().add(new ItemStack(Items.PAPER, 3));
            PhotoService.upload(player, picture());
            helper.assertValueEqual(player.getInventory().countItem(Items.PAPER), 2, "paper left");
            ItemStack photo = findPhoto(player);
            helper.assertFalse(photo.isEmpty(), "photo printed");
            PhotoData data = photo.get(FemboyComponents.PHOTO.get());
            helper.assertTrue(data != null && data.author().equals(player.getGameProfile().name()), "photo has id and author");
            PhotoIndex index = PhotoIndex.get(player.level().getServer());
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
            player.getInventory().add(new ItemStack(Items.PAPER, 3));
            PhotoService.upload(player, picture());
            helper.assertValueEqual(player.getInventory().countItem(Items.PAPER), 2, "garbage without paper wasn't decoded, so no wait");
            PhotoService.upload(player, picture());
            helper.assertValueEqual(player.getInventory().countItem(Items.PAPER), 2, "second print right away waits for the cooldown");
            PhotoService.forget(player);
            PhotoService.upload(player, new byte[]{1, 2, 3});
            helper.assertValueEqual(player.getInventory().countItem(Items.PAPER), 2, "broken data is not printed and costs nothing");
            PhotoService.upload(player, picture());
            helper.assertValueEqual(player.getInventory().countItem(Items.PAPER), 2, "after a broken upload the next one waits");
            PhotoService.forget(player);
        });
    }

    public static void photoStoredAndLoaded(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            PhotoService.upload(player, picture()); // creative: free
            PhotoData data = findPhoto(player).get(FemboyComponents.PHOTO.get());
            helper.assertTrue(data != null, "photo printed in creative without paper");
            byte[] stored = PhotoStore.load(player.level().getServer(), data.id()).join().orElseThrow();
            helper.assertTrue(PhotoCodec.decode(stored).isPresent(), "stored picture decodes");
            helper.assertTrue(PhotoStore.load(player.level().getServer(), java.util.UUID.randomUUID()).join().isEmpty(),
                    "unknown id: nothing");
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
        helper.assertValueEqual(index.add(a3, alice, 2, 4), List.of(a2), "her least recently seen fades, not the one just looked at");
        helper.assertValueEqual(index.count(alice), 2, "two of hers left");
        index.add(b1, bob, 2, 4);
        index.add(b2, bob, 2, 4);
        helper.assertValueEqual(index.add(b3, bob, 2, 4), List.of(b1), "his own cap takes his oldest, not hers");
        helper.assertValueEqual(index.add(c1, UUID.randomUUID(), 2, 3), List.of(a1, a3), "past the total the oldest of anyone go");
        helper.assertValueEqual(index.size(), 3, "three left");

        var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag saved = PhotoIndex.CODEC.encodeStart(ops, index).getOrThrow();
        PhotoIndex loaded = PhotoIndex.CODEC.parse(ops, saved).getOrThrow();
        helper.assertValueEqual(loaded.count(bob), 2, "authors survive a save");
        helper.assertValueEqual(loaded.add(UUID.randomUUID(), alice, 0, 3), List.of(b2), "and so does the order");

        WearableGameTests.withPlayer(helper, player -> {
            PhotoService.upload(player, picture()); // creative: free
            PhotoData data = findPhoto(player).get(FemboyComponents.PHOTO.get());
            helper.assertTrue(data != null, "photo printed");
            var server = player.level().getServer();
            PhotoStore.delete(server, data.id()).join();
            helper.assertTrue(PhotoStore.load(server, data.id()).join().isEmpty(), "a faded picture is gone from disk and cache");
            PhotoService.forget(player);
        });
    }
}
