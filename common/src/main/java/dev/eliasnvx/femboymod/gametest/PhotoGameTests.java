package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.photo.PhotoCodec;
import dev.eliasnvx.femboymod.photo.PhotoData;
import dev.eliasnvx.femboymod.photo.PhotoService;
import dev.eliasnvx.femboymod.photo.PhotoStore;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
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
            new CosmeticGameTests.Entry("photo_stored_and_loaded", PhotoGameTests::photoStoredAndLoaded));

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
            PhotoService.forget(player);
        });
    }

    public static void photoNeedsPaperAndWaits(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            player.setGameMode(GameType.SURVIVAL);
            PhotoService.upload(player, picture());
            helper.assertTrue(findPhoto(player).isEmpty(), "no paper, no photo");
            player.getInventory().add(new ItemStack(Items.PAPER, PAPER));
            PhotoService.upload(player, new byte[]{1, 2, 3});
            helper.assertTrue(findPhoto(player).isEmpty(), "broken data is not printed");
            GameTestAsserts.assertValueEqual(helper, player.getInventory().countItem(Items.PAPER), PAPER, "and costs nothing");
            PhotoService.upload(player, picture());
            PhotoService.upload(player, picture());
            GameTestAsserts.assertValueEqual(helper, player.getInventory().countItem(Items.PAPER), PAPER - 1,
                    "second print right away waits for the cooldown");
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
}
