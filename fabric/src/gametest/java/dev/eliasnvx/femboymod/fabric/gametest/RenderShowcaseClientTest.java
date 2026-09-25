package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.colorway.ColorwayPattern;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.Optional;

/**
 * Visual check of the placeholder cosmetics (SPEC §16 Phase 2): full set on the player, screenshots
 * from the front and back, standing and walking. Screenshots land in the run dir's screenshots folder.
 * Run with {@code ./gradlew :fabric:runClientGameTest}.
 */
public final class RenderShowcaseClientTest implements FabricClientGameTest {

    private static final int SETTLE_TICKS = 20;
    private static final int WALK_TICKS = 12;
    private static final int ZOOM_FOV = 45;
    private static final int DEFAULT_FOV = 70;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                wearFullSet(player);
            });
            context.waitTicks(SETTLE_TICKS);

            context.runOnClient(mc -> mc.options.fov().set(ZOOM_FOV));
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            context.waitTicks(SETTLE_TICKS);
            context.takeScreenshot("femboymod_front_idle");
            world.getServer().runCommand("tp @p ~ ~ ~ 35 0");
            context.waitTicks(SETTLE_TICKS / 2);
            context.takeScreenshot("femboymod_front_angled");
            world.getServer().runCommand("tp @p ~ ~ ~ 0 0");

            context.getInput().holdKeyFor(options -> options.keyUp, WALK_TICKS);
            context.takeScreenshot("femboymod_front_walking");

            context.getInput().holdShift();
            context.waitTicks(SETTLE_TICKS / 2);
            context.takeScreenshot("femboymod_front_crouching");
            context.getInput().releaseShift();

            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            context.waitTicks(SETTLE_TICKS);
            context.takeScreenshot("femboymod_back_idle");
            world.getServer().runCommand("tp @p ~ ~ ~ -40 0");
            context.waitTicks(SETTLE_TICKS / 2);
            context.takeScreenshot("femboymod_back_angled");
            context.getInput().holdKeyFor(options -> options.keyLeft, WALK_TICKS);
            context.takeScreenshot("femboymod_back_strafing");

            world.getServer().runOnServer(server -> CosmeticsManager.set(server.getPlayerList().getPlayers().getFirst(),
                    FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.HAIR_CLIPS.getFirst().get())));
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            context.waitTicks(SETTLE_TICKS);
            context.takeScreenshot("femboymod_front_hair_clip");
            world.getServer().runOnServer(server -> CosmeticsManager.set(server.getPlayerList().getPlayers().getFirst(),
                    FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get())));
            // Patterns: Progress chevron on the hoodie, striped ears, bi skirt, trans tail rings.
            world.getServer().runOnServer(server -> wearPatterns(server.getPlayerList().getPlayers().getFirst()));
            context.waitTicks(SETTLE_TICKS);
            context.takeScreenshot("femboymod_patterns_front");
            world.getServer().runCommand("tp @p ~ ~ ~ 35 0");
            context.waitTicks(SETTLE_TICKS / 2);
            context.takeScreenshot("femboymod_patterns_angled");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            context.waitTicks(SETTLE_TICKS / 2);
            context.takeScreenshot("femboymod_patterns_back");
            world.getServer().runCommand("tp @p ~ ~ ~ 0 0");
            context.runOnClient(mc -> mc.options.fov().set(DEFAULT_FOV));

            // Backpack with charms on the back (SPEC §5.3)
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ItemStack backpack = new ItemStack(FemboyItems.CANVAS_BACKPACK.get());
                backpack.set(FemboyComponents.CHARMS.get(), ItemContainerContents.fromItems(java.util.List.of(
                        new ItemStack(FemboyItems.SHARK_PLUSH_CHARM.get()), new ItemStack(FemboyItems.HEART_PIN.get()),
                        new ItemStack(FemboyItems.ENERGY_CAN_CHARM.get()))));
                backpack.set(net.minecraft.core.component.DataComponents.CONTAINER,
                        ItemContainerContents.fromItems(java.util.List.of(new ItemStack(net.minecraft.world.item.Items.CAKE))));
                CosmeticsManager.set(player, FemboySlots.BACK, backpack);
            });
            context.runOnClient(mc -> mc.options.fov().set(ZOOM_FOV));
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            world.getServer().runCommand("tp @p ~ ~ ~ -30 0");
            context.waitTicks(SETTLE_TICKS);
            context.takeScreenshot("femboymod_backpack_back");
            world.getServer().runCommand("tp @p ~ ~ ~ 90 0");
            context.waitTicks(SETTLE_TICKS / 2);
            context.takeScreenshot("femboymod_backpack_side");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            context.waitTicks(SETTLE_TICKS / 2);
            context.takeScreenshot("femboymod_backpack_front");
            world.getServer().runCommand("tp @p ~ ~ ~ 0 0");
            context.runOnClient(mc -> mc.options.fov().set(DEFAULT_FOV));

            // Backpack screen with the charm panel
            world.getServer().runOnServer(server -> dev.eliasnvx.femboymod.backpack.BackpackMenus.openFromKey(
                    server.getPlayerList().getPlayers().getFirst()));
            context.waitTicks(SETTLE_TICKS / 2);
            context.takeScreenshot("femboymod_backpack_screen");
            context.runOnClient(mc -> mc.player.closeContainer());
            context.waitTicks(SETTLE_TICKS / 4);

            // UwU choker: outgoing chat is rewritten before signing (SPEC §4.7); server log shows the result
            context.runOnClient(mc -> mc.player.connection.sendChat("привет друг"));
            context.runOnClient(mc -> mc.player.connection.sendChat("hello friend, see https://example.org/real"));
            context.runOnClient(mc -> mc.player.connection.sendCommand("say command stays hello"));
            context.waitTicks(SETTLE_TICKS);

            // First person: hoodie sleeve over the hand
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            context.waitTicks(SETTLE_TICKS / 2);
            context.takeScreenshot("femboymod_first_person_sleeve");

            // Cat Ears: hostile mobs within 16 blocks glow for the wearer.
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            world.getServer().runCommand("summon minecraft:zombie ~ ~ ~6 {NoAI:1b,PersistenceRequired:1b}");
            world.getServer().runCommand("tp @p ~ ~ ~ 0 10");
            context.waitTicks(SETTLE_TICKS);
            context.takeScreenshot("femboymod_glowing_zombie");
        }
    }

    private static ItemStack withPattern(ServerPlayer player, ItemStack stack, String pattern, int base, int secondary) {
        var patterns = player.level().registryAccess().lookupOrThrow(ColorwayPattern.REGISTRY_KEY);
        stack.set(FemboyComponents.COLORWAY.get(), new Colorway(base, Optional.of(patterns.getOrThrow(
                ResourceKey.create(ColorwayPattern.REGISTRY_KEY, Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, pattern)))),
                Optional.of(secondary)));
        return stack;
    }

    private static void wearPatterns(ServerPlayer player) {
        CosmeticsManager.set(player, FemboySlots.OUTFIT_TOP,
                withPattern(player, new ItemStack(FemboyItems.OVERSIZED_HOODIE.get()), "pride_progress", 0xFFFFFF, 0xFFFFFF));
        CosmeticsManager.set(player, FemboySlots.OUTFIT_BOTTOM,
                withPattern(player, new ItemStack(FemboyItems.PLEATED_SKIRT.get()), "pride_bi", 0xFFFFFF, 0xFFFFFF));
        CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY,
                withPattern(player, new ItemStack(FemboyItems.CAT_EARS.get()), "stripes", 0xF291BE, 0xFFFFFF));
        CosmeticsManager.set(player, FemboySlots.TAIL,
                withPattern(player, new ItemStack(FemboyItems.TAIL.get()), "pride_trans", 0xFFFFFF, 0xFFFFFF));
    }

    private static void wearFullSet(ServerPlayer player) {
        var patterns = player.level().registryAccess().lookupOrThrow(ColorwayPattern.REGISTRY_KEY);
        ItemStack socks = new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get());
        socks.set(FemboyComponents.COLORWAY.get(), new Colorway(0xFFFFFF, Optional.of(patterns.getOrThrow(
                ResourceKey.create(ColorwayPattern.REGISTRY_KEY, Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "pride_trans")))),
                Optional.empty()));
        CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get()));
        CosmeticsManager.set(player, FemboySlots.OUTFIT_TOP, new ItemStack(FemboyItems.OVERSIZED_HOODIE.get()));
        CosmeticsManager.set(player, FemboySlots.OUTFIT_BOTTOM, new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
        CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, socks);
        CosmeticsManager.set(player, FemboySlots.TAIL, new ItemStack(FemboyItems.TAIL.get()));
        CosmeticsManager.set(player, FemboySlots.NECK, new ItemStack(FemboyItems.UWU_CHOKER.get()));
    }
}
