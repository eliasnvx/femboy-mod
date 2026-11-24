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
 * Real in-game renders only.
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
        String shader = System.getenv("FEMBOYMOD_COMPAT_SHADER");
        if (shader != null && !shader.isBlank()) {
            context.runOnClient(mc -> {
                try {
                    CompatModelsClientTest.enableShaderPack(java.nio.file.Path.of(shader));
                } catch (ReflectiveOperationException | java.io.IOException e) {
                    throw new IllegalStateException("could not turn on the shader pack", e);
                }
            });
            context.waitTicks(200);
        }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("clear @p femboymod:lookbook"); // the first-join book, not wanted in shots
            cmd(world, "time set 1000"); // bright golden morning sunlight
            cmd(world, "weather clear");
            cmd(world, "gamerule advance_time false");
            cmd(world, "gamerule advance_weather false");
            context.runOnClient(mc -> mc.gui.toastManager().clear());

            garden(world);
            // 1. Outfits hero shot in a cherry garden in golden morning sun
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
            cmd(world, "setblock ~-2 ~ ~-3 femboymod:clothing_rack[facing=south]");
            cmd(world, "setblock ~2 ~ ~-3 femboymod:shark_plush[facing=south]");
            cmd(world, "summon femboymod:stray_cat ~2 ~ ~-1.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[200f,0f],femboymod_coat:0}");
            cmd(world, "summon femboymod:stray_cat ~-2 ~ ~-1 {NoAI:1b,PersistenceRequired:1b,Rotation:[160f,0f],femboymod_coat:1}");
            camera(context, world, CameraType.THIRD_PERSON_FRONT, 22, 10, 46);
            shot(context, "docs_hero");
            camera(context, world, CameraType.THIRD_PERSON_BACK, 152, 16, 48);
            shot(context, "docs_back");
            gone(world, "femboymod:stray_cat");
            clearGarden(world);

            // 2. Cozy Gamer Room at twilight (hero room + UI overlay)
            gamerRoom(world);
            outfit(world, player -> {
                wear(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EAR_HEADPHONES.get()));
                wear(player, FemboySlots.OUTFIT_TOP, colored(FemboyItems.CAT_EAR_HOODIE.get(), 0xF7B8D2));
                wear(player, FemboySlots.OUTFIT_BOTTOM, new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
                wear(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
                wear(player, FemboySlots.TAIL, new ItemStack(FemboyItems.TAIL.get()));
                wear(player, FemboySlots.NECK, new ItemStack(FemboyItems.UWU_CHOKER.get()));
                wear(player, FemboySlots.HANDS, new ItemStack(FemboyItems.STRIPED_MITTENS.get()));
                player.getInventory().add(new ItemStack(FemboyItems.BUBBLE_TEA.get(), 4));
                player.getInventory().add(new ItemStack(FemboyItems.PHONE.get()));
            });
            cmd(world, "time set 13100"); // warm twilight glow for neon + lamps + screens
            // Slightly angled wide shot so the keyboard+mouse on the desk are clearly visible beside the chair
            cmd(world, "tp @p ~-1.4 ~ ~0.4 166 10");
            camera(context, world, CameraType.FIRST_PERSON, 166, 10, 56);
            context.waitTicks(SETTLE);
            shot(context, "docs_gamer_room_wide");

            // 3. UI: Outfit menu centered over the glowing cozy gamer room (native window size so GUI centers cleanly)
            context.runOnClient(mc -> {
                mc.options.guiScale().set(2);
                mc.gui.toastManager().clear();
            });
            cmd(world, "kill @e[type=minecraft:item]");
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().openMenu(
                    new net.minecraft.world.SimpleMenuProvider((id, inventory, p) -> new dev.eliasnvx.femboymod.menu.CosmeticsMenu(id, inventory),
                            net.minecraft.network.chat.Component.translatable("container.femboymod.cosmetics"))));
            context.waitTicks(SETTLE / 2);
            context.takeScreenshot("docs_outfit_screen");
            context.runOnClient(mc -> mc.player.closeContainer());
            cmd(world, "tp @p 0.5 ~ 0.5 180 10");
            clearArea(world);

            // 4. Friends & Plushies Parade in cherry meadow in bright morning sun
            cmd(world, "time set 1000");
            outfit(world, player -> { });
            friendsScene(world);
            camera(context, world, CameraType.FIRST_PERSON, 180, 11, 50);
            context.waitTicks(SETTLE);
            shot(context, "docs_friends");
            gone(world, "femboymod:stray_cat");
            gone(world, "femboymod:cosplayer");
            clearArea(world);

            // 5. World: Plants (strawberry bush, catnip, flytrap), Rose Quartz geodes, Ores & Meme Mobs
            cmd(world, "time set 1200");
            worldScene(world);
            camera(context, world, CameraType.FIRST_PERSON, 180, 10, 54);
            context.waitTicks(SETTLE);
            shot(context, "docs_world");
            gone(world, "!minecraft:player");
            clearArea(world);
        }
    }

    static void cmd(TestSingleplayerContext world, String command) {
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

    interface Dresser {
        void dress(ServerPlayer player);
    }

    static void outfit(TestSingleplayerContext world, Dresser dresser) {
        world.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            for (var slot : CosmeticsManager.orderedSlots()) {
                CosmeticsManager.set(player, slot, ItemStack.EMPTY);
            }
            player.getInventory().clearContent();
            dresser.dress(player);
        });
    }

    static void wear(ServerPlayer player, Identifier slot, ItemStack stack) {
        CosmeticsManager.set(player, slot, stack);
    }

    static ItemStack colored(Item item, int rgb) {
        ItemStack stack = new ItemStack(item);
        stack.set(FemboyComponents.COLORWAY.get(), Colorway.solid(rgb));
        return stack;
    }

    static ItemStack patterned(MinecraftServer server, Item item, String pattern, int base) {
        ItemStack stack = new ItemStack(item);
        server.registryAccess().lookupOrThrow(ColorwayPattern.REGISTRY_KEY)
                .get(ResourceKey.create(ColorwayPattern.REGISTRY_KEY, Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, pattern)))
                .ifPresent(holder -> stack.set(FemboyComponents.COLORWAY.get(), new Colorway(base, Optional.of(holder), Optional.empty())));
        return stack;
    }

    static ItemStack backpack(MinecraftServer server) {
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
        cmd(world, "fill ~-12 ~ ~-12 ~12 ~8 ~6 air");
        cmd(world, "fill ~-12 ~-1 ~-12 ~12 ~-1 ~6 minecraft:grass_block");
        cmd(world, "kill @e[type=minecraft:item]");
        cmd(world, "kill @e[type=minecraft:painting]");
    }

    /** Removes entities without death poofs or drops (tp below the world). */
    static void gone(TestSingleplayerContext world, String type) {
        cmd(world, "tp @e[type=" + type + "] ~ -200 ~");
        cmd(world, "kill @e[type=minecraft:item]");
    }

    private static void gamerRoom(TestSingleplayerContext world) {
        // Floor: warm cherry planks + pink fluffy rug centerpiece
        cmd(world, "fill ~-6 ~-1 ~-8 ~6 ~-1 ~2 minecraft:cherry_planks");
        for (int x = -2; x <= 2; x++) {
            for (int z = -5; z <= -2; z++) {
                cmd(world, "setblock ~" + x + " ~ ~" + z + " femboymod:fluffy_rug[color=pink,shape=square]");
            }
        }
        // Walls & ceiling: clean smooth quartz + cherry log pillars + glass skylight/side window
        cmd(world, "fill ~-6 ~ ~-7 ~6 ~4 ~-7 minecraft:smooth_quartz");
        cmd(world, "fill ~-6 ~ ~-7 ~-6 ~4 ~2 minecraft:smooth_quartz");
        cmd(world, "fill ~6 ~ ~-7 ~6 ~4 ~2 minecraft:smooth_quartz");
        cmd(world, "fill ~-6 ~5 ~-7 ~6 ~5 ~2 minecraft:cherry_planks");
        // Corner pillars
        cmd(world, "fill ~-6 ~ ~-7 ~-6 ~4 ~-7 minecraft:stripped_cherry_log");
        cmd(world, "fill ~6 ~ ~-7 ~6 ~4 ~-7 minecraft:stripped_cherry_log");
        // Side window on east wall for warm twilight light
        cmd(world, "fill ~6 ~1 ~-5 ~6 ~3 ~-3 minecraft:glass_pane");

        // 5-wide Gamer Desk setup along the back wall
        for (int x = -2; x <= 2; x++) {
            cmd(world, "setblock ~" + x + " ~ ~-6 femboymod:gamer_desk[facing=south]");
        }
        // Desktop items: PC, Desk Mic, Monitor+Keyboard combo, 2nd Monitor, Ring Light, Plants & Duck
        cmd(world, "setblock ~-2 ~1 ~-6 femboymod:gaming_pc[facing=south,lit=true]");
        cmd(world, "setblock ~-1 ~1 ~-6 femboymod:gamer_monitor[facing=south]");
        cmd(world, "setblock ~ ~1 ~-6 femboymod:gamer_monitor[facing=south,with_keyboard=true]");
        cmd(world, "setblock ~1 ~1 ~-6 femboymod:ring_light[facing=south,lit=true]");
        cmd(world, "setblock ~2 ~1 ~-6 femboymod:sakura_bonsai[facing=south]");

        // Gamer chair facing the desk (pink wool under it so the rug has no harsh plank hole)
        cmd(world, "setblock ~ ~-1 ~-4 minecraft:pink_wool");
        cmd(world, "setblock ~ ~ ~-4 femboymod:gamer_chair[facing=north]");

        // Left side: Vanity Table, Bean Bag, Lava Lamp, Shark & Fox Plushies
        cmd(world, "setblock ~-5 ~ ~-4 femboymod:vanity_table[facing=east,half=lower]");
        cmd(world, "setblock ~-5 ~1 ~-4 femboymod:vanity_table[facing=east,half=upper]");
        cmd(world, "setblock ~-4 ~ ~-2 femboymod:bean_bag[color=pink,facing=east]");
        cmd(world, "setblock ~-4 ~ ~-6 femboymod:lava_lamp[color=pink,lit=true]");
        cmd(world, "setblock ~-3 ~ ~-6 femboymod:shark_plush[facing=south]");
        cmd(world, "setblock ~-5 ~ ~-6 femboymod:fox_plush[facing=south]");

        // Right side: Mini Fridge, Cat Succulent, Pet Bed, Moonstone Lamp, Pink Creeper & Cat Plushies
        cmd(world, "setblock ~5 ~ ~-6 femboymod:mini_fridge[facing=south]");
        cmd(world, "setblock ~5 ~1 ~-6 femboymod:cat_succulent[facing=south]");
        cmd(world, "setblock ~4 ~ ~-6 femboymod:pink_creeper_plush[facing=south]");
        cmd(world, "setblock ~3 ~ ~-6 femboymod:rubber_duck[facing=south]");
        cmd(world, "setblock ~4 ~ ~-3 femboymod:pet_bed[color=pink,facing=west]");
        cmd(world, "setblock ~5 ~ ~-2 femboymod:moonstone_lamp");
        cmd(world, "setblock ~3 ~ ~-2 femboymod:cat_plush[facing=north]");

        // Wall decor: LED strip, Fairy Lights, Neon Signs, Paintings
        for (int x = -5; x <= 5; x++) {
            cmd(world, "setblock ~" + x + " ~4 ~-6 femboymod:led_strip[facing=south,color=" + (x % 2 == 0 ? 4 : 0) + "]");
        }
        cmd(world, "setblock ~-4 ~2 ~-6 femboymod:neon_sign[facing=south,design=0]");
        cmd(world, "setblock ~4 ~2 ~-6 femboymod:neon_sign[facing=south,design=1]");
        cmd(world, "setblock ~-5 ~3 ~-5 femboymod:fairy_lights[facing=east,mode=0]");
        cmd(world, "setblock ~-5 ~3 ~-3 femboymod:fairy_lights[facing=east,mode=1]");
        cmd(world, "summon minecraft:painting ~-2 ~3 ~-6 {facing:0b,variant:\"femboymod:night_coding\"}");
        cmd(world, "summon minecraft:painting ~2 ~3 ~-6 {facing:0b,variant:\"femboymod:code_with_love\"}");
    }

    private static void friendsScene(TestSingleplayerContext world) {
        garden(world);
        // Clear petals from the porch area so cats & plushies stand cleanly on the cherry planks
        cmd(world, "fill ~-5 ~ ~-7 ~5 ~1 ~-1 air");
        cmd(world, "fill ~-5 ~-1 ~-7 ~5 ~-1 ~-2 minecraft:cherry_planks");
        cmd(world, "setblock ~-4 ~ ~-6 femboymod:bean_bag[color=magenta,facing=south]");
        cmd(world, "setblock ~4 ~ ~-6 femboymod:pet_bed[color=pink,facing=south]");
        cmd(world, "setblock ~-5 ~ ~-5 femboymod:moonstone_lamp");
        cmd(world, "setblock ~5 ~ ~-5 femboymod:moonstone_lamp");

        // Plushie parade on a low cherry slab shelf
        cmd(world, "fill ~-4 ~ ~-5 ~4 ~ ~-5 minecraft:cherry_slab[type=bottom]");
        String[] plushies = {
            "shark_plush", "pink_creeper_plush", "cat_plush", "fox_plush",
            "rubber_duck", "axolotl_plush", "bunny_plush", "red_panda_plush", "creeper_plush"
        };
        for (int i = 0; i < plushies.length; i++) {
            cmd(world, "setblock ~" + (i - 4) + " ~1 ~-5 femboymod:" + plushies[i] + "[facing=south]");
        }
        // Stray cats in all 5 coats + Cosplayer
        for (int coat = 0; coat < 5; coat++) {
            cmd(world, "summon femboymod:stray_cat ~" + ((coat - 2) * 1.4) + " ~ ~-3.2 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f],femboymod_coat:" + coat + "}");
        }
        cmd(world, "summon femboymod:cosplayer ~ ~ ~-6.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f]}");
    }

    private static void worldScene(TestSingleplayerContext world) {
        // Cherry backdrop trees
        tree(world, -6, -10);
        tree(world, 6, -10);
        cmd(world, "fill ~-8 ~ ~-9 ~8 ~ ~-1 minecraft:pink_petals[flower_amount=2] replace air");

        // Left: Plant garden (strawberry bushes, catnip, debug flytraps — clearly inside the frame)
        cmd(world, "fill ~-4 ~ ~-5 ~-2 ~ ~-3 air");
        cmd(world, "fill ~-4 ~-1 ~-5 ~-2 ~-1 ~-3 minecraft:farmland[moisture=7]");
        cmd(world, "setblock ~-4 ~ ~-4 femboymod:strawberry_bush[age=3]");
        cmd(world, "setblock ~-3 ~ ~-4 femboymod:strawberry_bush[age=3]");
        cmd(world, "setblock ~-4 ~ ~-3 femboymod:catnip[age=3]");
        cmd(world, "setblock ~-3 ~ ~-3 femboymod:catnip[age=3]");
        cmd(world, "setblock ~-2 ~ ~-4 femboymod:debug_flytrap[stage=2]");
        cmd(world, "setblock ~-2 ~ ~-3 femboymod:strawberry_bush[age=3]");

        // Right/back: Rose quartz geode & ores outcrop
        cmd(world, "fill ~2 ~ ~-7 ~5 ~2 ~-7 minecraft:deepslate");
        cmd(world, "setblock ~2 ~ ~-6 femboymod:budding_rose_quartz");
        cmd(world, "setblock ~3 ~ ~-6 femboymod:budding_rose_quartz");
        cmd(world, "setblock ~4 ~ ~-6 femboymod:budding_rose_quartz");
        cmd(world, "setblock ~2 ~1 ~-6 femboymod:large_rose_quartz_bud[facing=up]");
        cmd(world, "setblock ~3 ~1 ~-6 femboymod:rose_quartz_cluster[facing=up]");
        cmd(world, "setblock ~4 ~1 ~-6 femboymod:rose_quartz_cluster[facing=up]");
        cmd(world, "setblock ~2 ~1 ~-7 femboymod:rose_quartz_ore");
        cmd(world, "setblock ~3 ~2 ~-7 femboymod:deepslate_moonstone_ore");
        cmd(world, "setblock ~4 ~1 ~-7 femboymod:neon_quartz_ore");
        cmd(world, "setblock ~5 ~ ~-6 femboymod:moonstone_lamp");

        // Center: Meme Mobs showcase
        cmd(world, "summon femboymod:pink_creeper ~-0.8 ~ ~-5.2 {NoAI:1b,PersistenceRequired:1b,Rotation:[12f,0f]}");
        cmd(world, "summon femboymod:caffeinated_zombie ~0.7 ~ ~-5.8 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f],equipment:{mainhand:{id:\"femboymod:byte_energy_blue\",count:1}}}");
        cmd(world, "summon femboymod:fashion_critic ~2.1 ~ ~-5.0 {NoAI:1b,PersistenceRequired:1b,Rotation:[-18f,0f],equipment:{mainhand:{id:\"minecraft:writable_book\",count:1}}}");
        cmd(world, "summon femboymod:hissy_cat ~1.0 ~ ~-3.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[-12f,0f]}");
        cmd(world, "summon femboymod:bug ~-1.1 ~ ~-3.4 {NoAI:1b,PersistenceRequired:1b,Rotation:[20f,0f]}");
    }
}
