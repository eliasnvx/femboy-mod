package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.colorway.ColorwayPattern;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Screenshots for the README and the mod pages (docs/images). Skipped unless {@code FEMBOYMOD_DOCS_SHOTS=1}:
 * {@code FEMBOYMOD_DOCS_SHOTS=1 ./gradlew :fabric:runClientGameTest}, then {@code python3 tools/docs/make_readme_images.py}.
 * Real in-game renders only (no AI images on pages, AGENTS.md).
 */
public final class DocsShotsClientTest implements FabricClientGameTest {

    private static final int SETTLE = 20;
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("FEMBOYMOD_DOCS_SHOTS") == null) {
            return;
        }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            cmd(world, "time set noon");
            cmd(world, "weather clear");
            cmd(world, "gamerule advance_time false");
            cmd(world, "gamerule advance_weather false");
            context.runOnClient(mc -> mc.gui.toastManager().clear());

            garden(world);
            // Hero: the classic look in a flower garden with two stray cats
            outfit(world, player -> {
                wear(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get()));
                wear(player, FemboySlots.OUTFIT_TOP, colored(FemboyItems.OVERSIZED_HOODIE.get(), 0xF7B8D2));
                wear(player, FemboySlots.OUTFIT_BOTTOM, new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
                wear(player, FemboySlots.LEGS_OVERLAY, patterned(player.level().getServer(), FemboyItems.PROGRAMMING_SOCKS.get(), "pride_trans", 0xFFFFFF));
                wear(player, FemboySlots.TAIL, new ItemStack(FemboyItems.TAIL.get()));
                wear(player, FemboySlots.NECK, new ItemStack(FemboyItems.UWU_CHOKER.get()));
                wear(player, FemboySlots.HANDS, new ItemStack(FemboyItems.STRIPED_MITTENS.get()));
                wear(player, FemboySlots.BACK, backpack(player.level().getServer()));
            });
            cmd(world, "summon femboymod:stray_cat ~2 ~ ~-2 {NoAI:1b,PersistenceRequired:1b,Rotation:[200f,0f],femboymod_coat:0}");
            cmd(world, "summon femboymod:stray_cat ~-2 ~ ~-1 {NoAI:1b,PersistenceRequired:1b,Rotation:[160f,0f],femboymod_coat:1}");
            camera(context, world, CameraType.THIRD_PERSON_FRONT, 25, 12, 55);
            shot(context, "docs_hero");
            camera(context, world, CameraType.THIRD_PERSON_BACK, 150, 18, 55);
            shot(context, "docs_back");
            gone(world, "femboymod:stray_cat");

            // Outfits
            outfit(world, player -> {
                wear(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EAR_HEADPHONES.get()));
                wear(player, FemboySlots.FACE, new ItemStack(FemboyItems.DARK_SHADES.get()));
                wear(player, FemboySlots.OUTFIT_TOP, colored(FemboyItems.CROP_SWEATER.get(), 0x2B2A33));
                wear(player, FemboySlots.WAIST, new ItemStack(FemboyItems.BELT_CHAINS.get()));
                wear(player, FemboySlots.OUTFIT_BOTTOM, colored(FemboyItems.PLEATED_SKIRT.get(), 0x2B2A33));
                wear(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.FISHNET_TIGHTS.get()));
                wear(player, FemboySlots.HANDS, colored(FemboyItems.ARM_WARMERS.get(), 0x2B2A33));
            });
            camera(context, world, CameraType.THIRD_PERSON_FRONT, 20, 8, 50);
            shot(context, "docs_outfit_edgy");
            outfit(world, player -> {
                wear(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.BUNNY_EARS.get()));
                wear(player, FemboySlots.NECK, new ItemStack(FemboyItems.MOONSTONE_PENDANT.get()));
                wear(player, FemboySlots.OUTFIT_TOP, patterned(player.level().getServer(), FemboyItems.CAT_EAR_HOODIE.get(), "sakura", 0xFFFFFF));
                wear(player, FemboySlots.OUTFIT_BOTTOM, colored(FemboyItems.PLEATED_SKIRT.get(), 0xF2F0F2));
                wear(player, FemboySlots.LEGS_OVERLAY, patterned(player.level().getServer(), FemboyItems.PROGRAMMING_SOCKS.get(), "pearl", 0xFFFFFF));
                wear(player, FemboySlots.FACE, new ItemStack(FemboyItems.HEART_GLASSES.get()));
            });
            shot(context, "docs_outfit_soft");
            outfit(world, player -> {
                wear(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.FOX_EARS.get()));
                wear(player, FemboySlots.FACE, patterned(player.level().getServer(), FemboyItems.CYBER_VISOR.get(), "neon", 0xFFFFFF));
                wear(player, FemboySlots.OUTFIT_TOP, patterned(player.level().getServer(), FemboyItems.OVERSIZED_HOODIE.get(), "cyber_pastel", 0xFFFFFF));
                wear(player, FemboySlots.OUTFIT_BOTTOM, colored(FemboyItems.PLEATED_SKIRT.get(), 0x1E1A2A));
                wear(player, FemboySlots.LEGS_OVERLAY, patterned(player.level().getServer(), FemboyItems.PROGRAMMING_SOCKS.get(), "neon", 0xFFFFFF));
                wear(player, FemboySlots.TAIL, colored(FemboyItems.TAIL.get(), 0xE8863A));
            });
            shot(context, "docs_outfit_cyber");
            outfit(world, player -> {
                wear(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.BEAR_EARS.get()));
                wear(player, FemboySlots.FACE, new ItemStack(FemboyItems.ROSE_QUARTZ_EARRINGS.get()));
                wear(player, FemboySlots.OUTFIT_TOP, patterned(player.level().getServer(), FemboyItems.OVERSIZED_HOODIE.get(), "pride_rainbow", 0xFFFFFF));
                wear(player, FemboySlots.OUTFIT_BOTTOM, colored(FemboyItems.PLEATED_SKIRT.get(), 0xA8E6CF));
                wear(player, FemboySlots.LEGS_OVERLAY, patterned(player.level().getServer(), FemboyItems.PROGRAMMING_SOCKS.get(), "glitter", 0xFFD1EC));
                wear(player, FemboySlots.HANDS, new ItemStack(FemboyItems.ROSE_QUARTZ_BRACELET.get()));
            });
            shot(context, "docs_outfit_pride");

            // Emote
            context.runOnClient(mc -> dev.eliasnvx.femboymod.client.EmoteClient.show(mc.player.getId(), dev.eliasnvx.femboymod.emote.Emote.HEART_HANDS));
            context.waitTicks(SETTLE / 2);
            shot(context, "docs_emote");
            clearGarden(world);

            // Gamer room at night
            gamerRoom(world);
            outfit(world, player -> { });
            outfit(world, player -> {
                wear(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EAR_HEADPHONES.get()));
                wear(player, FemboySlots.OUTFIT_TOP, colored(FemboyItems.CAT_EAR_HOODIE.get(), 0xC8A2E8));
                wear(player, FemboySlots.OUTFIT_BOTTOM, new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
                wear(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
                wear(player, FemboySlots.TAIL, new ItemStack(FemboyItems.TAIL.get()));
            });
            cmd(world, "time set midnight");
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                dev.eliasnvx.femboymod.entity.Seat.sit(player.level(), player.blockPosition().offset(0, 0, -3), player, 0.3);
            });
            camera(context, world, CameraType.THIRD_PERSON_BACK, 180, 20, 70);
            context.waitTicks(SETTLE);
            shot(context, "docs_gamer_room");
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().stopRiding());
            outfit(world, player -> { });
            cmd(world, "tp @p ~ ~ ~ 180 10");
            camera(context, world, CameraType.FIRST_PERSON, 180, 8, 70);
            shot(context, "docs_gamer_room_wide");
            clearArea(world);
            cmd(world, "time set noon");

            // Ores, geodes and neon at night (no outfit: set-bonus hearts would float in front of the camera)
            outfit(world, player -> { });
            oreWall(world);
            cmd(world, "time set midnight");
            camera(context, world, CameraType.FIRST_PERSON, 180, 12, 70);
            shot(context, "docs_ores");
            clearArea(world);
            cmd(world, "time set noon");

            // Friends and mobs
            for (int coat = 0; coat < 5; coat++) {
                cmd(world, "summon femboymod:stray_cat ~" + (coat - 2) * 1.2 + " ~ ~-3 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f],femboymod_coat:" + coat + "}");
            }
            cmd(world, "summon femboymod:cosplayer ~ ~ ~-5 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f]}");
            cmd(world, "setblock ~3 ~ ~-5 femboymod:vibe_scanner[facing=south]");
            camera(context, world, CameraType.FIRST_PERSON, 180, 14, 70);
            context.waitTicks(SETTLE);
            shot(context, "docs_friends");
            gone(world, "femboymod:stray_cat");
            gone(world, "femboymod:cosplayer");
            cmd(world, "setblock ~3 ~ ~-5 air");
            context.waitTicks(SETTLE * 2);
            cmd(world, "summon femboymod:pink_creeper ~-3 ~ ~-5 {NoAI:1b,PersistenceRequired:1b,Rotation:[10f,0f]}");
            cmd(world, "summon femboymod:caffeinated_zombie ~-1.5 ~ ~-5.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[5f,0f],equipment:{mainhand:{id:\"femboymod:byte_energy_blue\",count:1}}}");
            cmd(world, "summon femboymod:fashion_critic ~0.5 ~ ~-6 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f],equipment:{mainhand:{id:\"minecraft:writable_book\",count:1}}}");
            cmd(world, "summon femboymod:hissy_cat ~2.2 ~ ~-4.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[-15f,0f]}");
            cmd(world, "summon femboymod:bug ~3.5 ~ ~-4 {NoAI:1b,PersistenceRequired:1b,Rotation:[-30f,0f]}");
            cmd(world, "summon femboymod:bug ~0 ~ ~-3.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f]}");
            cmd(world, "time set noon");
            camera(context, world, CameraType.FIRST_PERSON, 180, 8, 70);
            context.waitTicks(SETTLE);
            shot(context, "docs_mobs");
            gone(world, "!minecraft:player");
            cmd(world, "time set noon");

            // UI: HUD panel + Outfit screen, and the creative tab
            outfit(world, player -> {
                wear(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get()));
                wear(player, FemboySlots.OUTFIT_TOP, colored(FemboyItems.OVERSIZED_HOODIE.get(), 0xF7B8D2));
                wear(player, FemboySlots.OUTFIT_BOTTOM, new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
                wear(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
                wear(player, FemboySlots.TAIL, new ItemStack(FemboyItems.TAIL.get()));
                wear(player, FemboySlots.NECK, new ItemStack(FemboyItems.UWU_CHOKER.get()));
                wear(player, FemboySlots.HANDS, new ItemStack(FemboyItems.STRIPED_MITTENS.get()));
                player.getInventory().add(new ItemStack(FemboyItems.BUBBLE_TEA.get(), 4));
                player.getInventory().add(new ItemStack(FemboyItems.PHONE.get()));
            });
            context.runOnClient(mc -> mc.options.guiScale().set(2));
            cmd(world, "kill @e[type=minecraft:item]");
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().openMenu(
                    new net.minecraft.world.SimpleMenuProvider((id, inventory, p) -> new dev.eliasnvx.femboymod.menu.CosmeticsMenu(id, inventory),
                            net.minecraft.network.chat.Component.translatable("container.femboymod.cosmetics"))));
            context.waitTicks(SETTLE / 2);
            context.takeScreenshot("docs_outfit_screen"); // native window size: GUI layout matches
            context.runOnClient(mc -> mc.player.closeContainer());
            cmd(world, "gamemode creative @p");
            context.waitTicks(SETTLE / 4);
            context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(
                    Minecraft.getInstance().player, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS, true));
            context.waitTicks(SETTLE / 2);
            context.takeScreenshot("docs_creative");
            context.setScreen(() -> null);
            cmd(world, "gamemode survival @p");

            // Photo Mode selfie (saved by the mod itself as an instant print)
            camera(context, world, CameraType.FIRST_PERSON, 200, 10, 70);
            context.runOnClient(mc -> dev.eliasnvx.femboymod.client.PhotoMode.start());
            context.waitTicks(SETTLE * 2);
        }
    }

    private static void cmd(TestSingleplayerContext world, String command) {
        world.getServer().runCommand(command);
    }

    private static void camera(ClientGameTestContext context, TestSingleplayerContext world, CameraType type, float yaw, float pitch, int fov) {
        cmd(world, "tp @p ~ ~ ~ " + yaw + " " + pitch);
        context.runOnClient(mc -> {
            mc.options.setCameraType(type);
            mc.options.fov().set(fov);
        });
        context.waitTicks(SETTLE / 2);
    }

    private static void shot(ClientGameTestContext context, String name) {
        context.runOnClient(mc -> {
            mc.gui.toastManager().clear();
            if (!mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
        });
        context.waitTicks(SETTLE / 4);
        context.takeScreenshot(TestScreenshotOptions.of(name).withSize(WIDTH, HEIGHT));
        context.runOnClient(mc -> mc.gui.hud.toggle());
    }

    private interface Dresser {
        void dress(ServerPlayer player);
    }

    private static void outfit(TestSingleplayerContext world, Dresser dresser) {
        world.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            for (var slot : CosmeticsManager.orderedSlots()) {
                CosmeticsManager.set(player, slot, ItemStack.EMPTY);
            }
            player.getInventory().clearContent();
            dresser.dress(player);
        });
    }

    private static void wear(ServerPlayer player, Identifier slot, ItemStack stack) {
        CosmeticsManager.set(player, slot, stack);
    }

    private static ItemStack colored(Item item, int rgb) {
        ItemStack stack = new ItemStack(item);
        stack.set(FemboyComponents.COLORWAY.get(), Colorway.solid(rgb));
        return stack;
    }

    private static ItemStack patterned(MinecraftServer server, Item item, String pattern, int base) {
        ItemStack stack = new ItemStack(item);
        server.registryAccess().lookupOrThrow(ColorwayPattern.REGISTRY_KEY)
                .get(ResourceKey.create(ColorwayPattern.REGISTRY_KEY, Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, pattern)))
                .ifPresent(holder -> stack.set(FemboyComponents.COLORWAY.get(), new Colorway(base, Optional.of(holder), Optional.empty())));
        return stack;
    }

    private static ItemStack backpack(MinecraftServer server) {
        ItemStack backpack = colored(FemboyItems.CANVAS_BACKPACK.get(), 0xF4A6C8);
        ItemStack badge = patterned(server, FemboyItems.PRIDE_BADGE.get(), "pride_trans", 0xFFFFFF);
        backpack.set(FemboyComponents.CHARMS.get(), ItemContainerContents.fromItems(List.of(badge,
                new ItemStack(FemboyItems.SHARK_PLUSH_CHARM.get()), new ItemStack(FemboyItems.HEART_PIN.get()))));
        return backpack;
    }

    private static void garden(TestSingleplayerContext world) {
        cmd(world, "fill ~-12 ~ ~-12 ~12 ~ ~12 minecraft:pink_petals[flower_amount=4] replace air");
        cmd(world, "fill ~-1 ~ ~-1 ~1 ~ ~1 air");
        for (int[] p : new int[][]{{-5, -6}, {6, -7}, {-8, -2}, {7, 1}, {-4, 6}, {3, 7}}) {
            cmd(world, "setblock ~" + p[0] + " ~ ~" + p[1] + " minecraft:peony[half=lower]");
            cmd(world, "setblock ~" + p[0] + " ~1 ~" + p[1] + " minecraft:peony[half=upper]");
        }
        tree(world, -4, -9);
        tree(world, 6, -11);
        tree(world, -10, -3);
        tree(world, 9, -2);
    }

    /** A small cherry tree built from blocks: trunk, a round pink crown. */
    private static void tree(TestSingleplayerContext world, int x, int z) {
        cmd(world, "fill ~" + x + " ~ ~" + z + " ~" + x + " ~3 ~" + z + " minecraft:cherry_log");
        cmd(world, "fill ~" + (x - 2) + " ~3 ~" + (z - 2) + " ~" + (x + 2) + " ~4 ~" + (z + 2) + " minecraft:cherry_leaves[persistent=true] replace air");
        cmd(world, "fill ~" + (x - 1) + " ~5 ~" + (z - 1) + " ~" + (x + 1) + " ~5 ~" + (z + 1) + " minecraft:cherry_leaves[persistent=true]");
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            cmd(world, "setblock ~" + (x + c[0]) + " ~4 ~" + (z + c[1]) + " air");
        }
    }

    private static void clearGarden(TestSingleplayerContext world) {
        cmd(world, "fill ~-14 ~ ~-14 ~14 ~12 ~14 air");
        cmd(world, "kill @e[type=minecraft:item]");
    }

    private static void clearArea(TestSingleplayerContext world) {
        cmd(world, "fill ~-8 ~ ~-9 ~8 ~6 ~2 air");
        cmd(world, "fill ~-8 ~-1 ~-9 ~8 ~-1 ~2 minecraft:grass_block");
        cmd(world, "kill @e[type=minecraft:item]");
        cmd(world, "kill @e[type=minecraft:painting]");
    }

    /** Removes entities without death poofs or drops (tp below the world). */
    private static void gone(TestSingleplayerContext world, String type) {
        cmd(world, "tp @e[type=" + type + "] ~ -200 ~");
        cmd(world, "kill @e[type=minecraft:item]");
    }

    private static void gamerRoom(TestSingleplayerContext world) {
        cmd(world, "fill ~-6 ~-1 ~-8 ~6 ~-1 ~1 minecraft:pink_wool");
        cmd(world, "fill ~-6 ~ ~-7 ~6 ~4 ~-7 minecraft:white_concrete");
        cmd(world, "fill ~-6 ~ ~-7 ~-6 ~4 ~1 minecraft:white_concrete");
        cmd(world, "fill ~6 ~ ~-7 ~6 ~4 ~1 minecraft:white_concrete");
        cmd(world, "fill ~-2 ~ ~-6 ~2 ~ ~-5 minecraft:pink_concrete");
        cmd(world, "setblock ~-1 ~1 ~-6 femboymod:gamer_monitor[facing=south]");
        cmd(world, "setblock ~1 ~1 ~-6 femboymod:gamer_monitor[facing=south]");
        cmd(world, "setblock ~ ~1 ~-5 femboymod:gamer_keyboard[facing=south]");
        cmd(world, "setblock ~2 ~1 ~-5 femboymod:rubber_duck[facing=south]");
        cmd(world, "setblock ~-2 ~1 ~-5 femboymod:terminal[facing=south]");
        cmd(world, "setblock ~ ~ ~-3 femboymod:gamer_chair[facing=north]");
        for (int x = -5; x <= 5; x++) {
            cmd(world, "setblock ~" + x + " ~4 ~-6 femboymod:led_strip[facing=south,color=" + (x % 2 == 0 ? 4 : 0) + "]");
        }
        cmd(world, "setblock ~-4 ~ ~-6 femboymod:shark_plush[facing=south]");
        cmd(world, "setblock ~4 ~ ~-6 femboymod:cat_plush[facing=south]");
        cmd(world, "setblock ~5 ~ ~-3 femboymod:moonstone_lamp");
        cmd(world, "setblock ~-5 ~2 ~-6 femboymod:neon_sign[facing=south,design=0]");
        cmd(world, "summon minecraft:painting ~3 ~3 ~-6 {facing:0b,variant:\"femboymod:code_with_love\"}");
        cmd(world, "summon minecraft:painting ~-3 ~3 ~-6 {facing:0b,variant:\"femboymod:night_coding\"}");
        cmd(world, "summon minecraft:painting ~5 ~2 ~-6 {facing:0b,variant:\"femboymod:btw\"}");
    }

    private static void oreWall(TestSingleplayerContext world) {
        cmd(world, "fill ~-5 ~ ~-7 ~5 ~4 ~-7 minecraft:deepslate");
        cmd(world, "fill ~-5 ~-1 ~-6 ~5 ~-1 ~-3 minecraft:smooth_basalt");
        cmd(world, "fill ~-4 ~ ~-6 ~-2 ~ ~-6 minecraft:calcite");
        cmd(world, "fill ~-1 ~ ~-6 ~3 ~ ~-6 femboymod:budding_rose_quartz");
        cmd(world, "setblock ~-1 ~1 ~-6 femboymod:small_rose_quartz_bud[facing=up]");
        cmd(world, "setblock ~ ~1 ~-6 femboymod:medium_rose_quartz_bud[facing=up]");
        cmd(world, "setblock ~1 ~1 ~-6 femboymod:large_rose_quartz_bud[facing=up]");
        cmd(world, "setblock ~2 ~1 ~-6 femboymod:rose_quartz_cluster[facing=up]");
        cmd(world, "setblock ~3 ~1 ~-6 femboymod:rose_quartz_cluster[facing=up]");
        cmd(world, "setblock ~-4 ~1 ~-7 femboymod:deepslate_moonstone_ore");
        cmd(world, "setblock ~-3 ~2 ~-7 femboymod:deepslate_moonstone_ore");
        cmd(world, "setblock ~4 ~1 ~-7 femboymod:neon_quartz_ore");
        cmd(world, "setblock ~4 ~2 ~-7 femboymod:neon_quartz_ore");
        cmd(world, "setblock ~-2 ~2 ~-7 femboymod:rose_quartz_ore");
        cmd(world, "setblock ~2 ~2 ~-7 femboymod:glitter_ore");
        cmd(world, "setblock ~5 ~ ~-6 femboymod:moonstone_lamp");
        for (int design = 0; design < 3; design++) {
            cmd(world, "setblock ~" + (design * 2 - 2) + " ~3 ~-6 femboymod:neon_sign[facing=south,design=" + design + "]");
        }
    }
}
