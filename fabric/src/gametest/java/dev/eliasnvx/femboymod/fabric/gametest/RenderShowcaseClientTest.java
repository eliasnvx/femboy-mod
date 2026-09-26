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
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Visual check of the placeholder cosmetics (SPEC §16 Phase 2): full set on the player, screenshots
 * from the front and back, standing and walking. Screenshots land in the run dir's screenshots folder.
 * Run with {@code ./gradlew :fabric:runClientGameTest}.
 */
public final class RenderShowcaseClientTest implements FabricClientGameTest {

    private static final int SETTLE_TICKS = 20;
    private static final int WALK_TICKS = 12;
    private static final int ZOOM_FOV = 45;
    /** Minimum FOV: close-ups for judging texture detail. */
    private static final int CLOSE_UP_FOV = 30;
    private static final int CLOSE_UP_WIDTH = 1920;
    private static final int CLOSE_UP_HEIGHT = 1080;
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
            closeUp(context, "femboymod_closeup_front");
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
            closeUp(context, "femboymod_closeup_back");
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
            // Cat ear hoodie: hood up, so no headband
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, ItemStack.EMPTY);
                CosmeticsManager.set(player, FemboySlots.OUTFIT_TOP, new ItemStack(FemboyItems.CAT_EAR_HOODIE.get()));
            });
            context.waitTicks(SETTLE_TICKS / 2);
            closeUp(context, "femboymod_closeup_cat_ear_hoodie_front");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            world.getServer().runCommand("tp @p ~ ~ ~ -30 0");
            context.waitTicks(SETTLE_TICKS / 2);
            closeUp(context, "femboymod_closeup_cat_ear_hoodie_back");
            world.getServer().runCommand("tp @p ~ ~ ~ 0 0");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            world.getServer().runOnServer(server -> CosmeticsManager.set(server.getPlayerList().getPlayers().getFirst(),
                    FemboySlots.OUTFIT_TOP, new ItemStack(FemboyItems.OVERSIZED_HOODIE.get())));
            // Animal ears
            for (var ears : List.of(FemboyItems.FOX_EARS, FemboyItems.BUNNY_EARS, FemboyItems.BEAR_EARS, FemboyItems.WOLF_EARS)) {
                world.getServer().runOnServer(server -> CosmeticsManager.set(server.getPlayerList().getPlayers().getFirst(),
                        FemboySlots.HEAD_ACCESSORY, new ItemStack(ears.get())));
                context.waitTicks(SETTLE_TICKS / 2);
                closeUp(context, "femboymod_closeup_" + ears.getId().getPath());
            }
            world.getServer().runOnServer(server -> CosmeticsManager.set(server.getPlayerList().getPlayers().getFirst(),
                    FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get())));
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

            // Phase 4: world content in front of the player (camera looks north)
            world.getServer().runCommand("fill ~-4 ~ ~-6 ~4 ~3 ~-2 air");
            world.getServer().runCommand("setblock ~-2 ~ ~-4 femboymod:clothing_rack[facing=south]");
            world.getServer().runCommand("setblock ~ ~ ~-5 femboymod:wardrobe[facing=south]");
            world.getServer().runCommand("setblock ~2 ~ ~-4 femboymod:shark_plush[facing=south]");
            world.getServer().runCommand("setblock ~3 ~ ~-3 femboymod:cat_plush[facing=south]");
            world.getServer().runCommand("setblock ~1 ~ ~-3 femboymod:creeper_plush[facing=south]");
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                var level = player.level();
                var pos = player.blockPosition().offset(-2, 0, -4);
                if (level.getBlockEntity(pos) instanceof dev.eliasnvx.femboymod.block.ClothingRackBlockEntity rack) {
                    rack.hang(new ItemStack(FemboyItems.OVERSIZED_HOODIE.get()));
                    rack.hang(new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
                    rack.hang(new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
                }
            });
            world.getServer().runCommand("summon femboymod:pink_creeper ~-1 ~ ~-2 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}");
            world.getServer().runCommand("summon minecraft:villager ~3 ~ ~-5 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f],VillagerData:{profession:\"femboymod:thrifter\",level:2,type:\"minecraft:plains\"}}");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            world.getServer().runCommand("tp @p ~ ~ ~ 180 20"); // yaw 180 = facing north
            context.waitTicks(SETTLE_TICKS * 2);
            context.takeScreenshot("femboymod_world_content");
            world.getServer().runCommand("kill @e[type=femboymod:pink_creeper]");
            world.getServer().runCommand("tp @p ~ ~ ~ 200 25");
            context.waitTicks(SETTLE_TICKS);
            context.takeScreenshot("femboymod_clothing_rack");

            // Hostile meme mobs
            world.getServer().runCommand("summon femboymod:bug ~-1 ~ ~-4 {NoAI:1b,PersistenceRequired:1b,Rotation:[-20f,0f]}");
            world.getServer().runCommand("summon femboymod:bug ~0.3 ~ ~-3.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[10f,0f]}");
            world.getServer().runCommand("summon femboymod:bug ~1.5 ~ ~-4.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[30f,0f]}");
            world.getServer().runCommand("tp @p ~ ~ ~ 180 22");
            context.waitTicks(SETTLE_TICKS);
            closeUp(context, "femboymod_closeup_bugs");
            world.getServer().runCommand("kill @e[type=femboymod:bug]");
            world.getServer().runCommand("summon femboymod:caffeinated_zombie ~ ~ ~-3.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f],equipment:{mainhand:{id:\"femboymod:byte_energy_blue\",count:1}}}");
            world.getServer().runCommand("tp @p ~ ~ ~ 180 0");
            context.waitTicks(SETTLE_TICKS * 2);
            closeUp(context, "femboymod_closeup_caffeinated_zombie");
            world.getServer().runCommand("kill @e[type=femboymod:caffeinated_zombie]");
            context.waitTicks(SETTLE_TICKS);
            world.getServer().runCommand("summon femboymod:caffeinated_zombie ~-1.2 ~ ~-6 {NoAI:1b,PersistenceRequired:1b,Rotation:[10f,0f],equipment:{mainhand:{id:\"femboymod:byte_energy_pink\",count:1}}}");
            world.getServer().runCommand("summon femboymod:hissy_cat ~0.8 ~ ~-3.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[-15f,0f]}");
            world.getServer().runCommand("time set midnight");
            world.getServer().runCommand("tp @p ~ ~ ~ 180 8");
            context.waitTicks(SETTLE_TICKS * 2); // let the bugs' death particles fade
            closeUp(context, "femboymod_closeup_meme_mobs");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("kill @e[type=femboymod:caffeinated_zombie]");
            world.getServer().runCommand("kill @e[type=femboymod:hissy_cat]");
            world.getServer().runCommand("summon femboymod:fashion_critic ~ ~ ~-4 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f],equipment:{mainhand:{id:\"minecraft:writable_book\",count:1}}}");
            world.getServer().runCommand("tp @p ~ ~ ~ 180 -5");
            context.waitTicks(SETTLE_TICKS);
            closeUp(context, "femboymod_closeup_fashion_critic");
            world.getServer().runCommand("kill @e[type=femboymod:fashion_critic]");

            // Gamer corner: desk with monitor and keyboard, chair, LED strips, posters, ores
            world.getServer().runCommand("fill ~-4 ~ ~-7 ~4 ~4 ~-1 air");
            world.getServer().runCommand("fill ~-4 ~ ~-6 ~4 ~3 ~-6 white_concrete");
            world.getServer().runCommand("fill ~-1 ~ ~-5 ~1 ~ ~-4 pink_concrete");
            world.getServer().runCommand("setblock ~ ~1 ~-5 femboymod:gamer_monitor[facing=south]");
            world.getServer().runCommand("setblock ~-1 ~1 ~-5 femboymod:gamer_monitor[facing=south,lit=false]");
            world.getServer().runCommand("setblock ~ ~1 ~-4 femboymod:gamer_keyboard[facing=south]");
            world.getServer().runCommand("setblock ~ ~ ~-3 femboymod:gamer_chair[facing=north]");
            for (int x = -4; x <= 4; x++) {
                world.getServer().runCommand("setblock ~" + x + " ~3 ~-5 femboymod:led_strip[facing=south,color=" + Math.floorMod(x, 5) + "]");
            }
            world.getServer().runCommand("setblock ~-3 ~ ~-5 femboymod:rose_quartz_ore");
            world.getServer().runCommand("setblock ~-3 ~1 ~-5 femboymod:deepslate_rose_quartz_ore");
            world.getServer().runCommand("setblock ~3 ~ ~-5 femboymod:glitter_ore");
            world.getServer().runCommand("setblock ~3 ~1 ~-5 femboymod:rose_quartz_block");
            world.getServer().runCommand("summon minecraft:painting ~-3 ~2 ~-5 {facing:0b,variant:\"femboymod:btw\"}");
            world.getServer().runCommand("summon minecraft:painting ~2 ~2 ~-5 {facing:0b,variant:\"femboymod:code_with_love\"}");
            world.getServer().runCommand("summon minecraft:painting ~4 ~1 ~-5 {facing:0b,variant:\"femboymod:stay_warm\"}");
            world.getServer().runCommand("tp @p ~ ~ ~ 180 15");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            context.waitTicks(SETTLE_TICKS * 2);
            closeUp(context, "femboymod_closeup_gamer_corner");
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                dev.eliasnvx.femboymod.entity.Seat.sit(player.level(), player.blockPosition().offset(0, 0, -3), player, 0.3);
            });
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            context.waitTicks(SETTLE_TICKS);
            closeUp(context, "femboymod_closeup_gamer_chair_sitting");
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().stopRiding());
            world.getServer().runCommand("kill @e[type=minecraft:painting]");
            world.getServer().runCommand("fill ~-4 ~ ~-7 ~4 ~4 ~-1 air");

            // Rose quartz jewelry and the shimmering glitter colorway
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, ItemStack.EMPTY);
                CosmeticsManager.set(player, FemboySlots.FACE, new ItemStack(FemboyItems.ROSE_QUARTZ_EARRINGS.get()));
                CosmeticsManager.set(player, FemboySlots.HANDS, new ItemStack(FemboyItems.ROSE_QUARTZ_BRACELET.get()));
                ItemStack socks = new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get());
                server.registryAccess().lookupOrThrow(ColorwayPattern.REGISTRY_KEY)
                        .get(ResourceKey.create(ColorwayPattern.REGISTRY_KEY, Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "glitter")))
                        .ifPresent(glitter -> socks.set(FemboyComponents.COLORWAY.get(),
                                new Colorway(0xFFD1EC, Optional.of(glitter), Optional.empty())));
                CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, socks);
            });
            world.getServer().runCommand("tp @p ~ ~ ~ 20 10");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            context.waitTicks(SETTLE_TICKS);
            closeUp(context, "femboymod_closeup_jewelry_glitter");
            context.waitTicks(SETTLE_TICKS);
            closeUp(context, "femboymod_closeup_jewelry_glitter_later");
            // Armor under the outfit: auto-hidden where cosmetics cover it; the game rule can force it visible
            world.getServer().runCommand("item replace entity @p armor.head with minecraft:iron_helmet");
            world.getServer().runCommand("item replace entity @p armor.chest with minecraft:diamond_chestplate");
            world.getServer().runCommand("item replace entity @p armor.legs with minecraft:iron_leggings");
            world.getServer().runCommand("item replace entity @p armor.feet with minecraft:golden_boots");
            context.waitTicks(SETTLE_TICKS / 2);
            closeUp(context, "femboymod_closeup_armor_hidden");
            world.getServer().runCommand("gamerule femboymod:allow_hidden_armor false");
            context.waitTicks(SETTLE_TICKS / 2);
            closeUp(context, "femboymod_closeup_armor_forced_visible");
            world.getServer().runCommand("gamerule femboymod:allow_hidden_armor true");
            // v1.1 clothing: headphones, heart glasses, crop sweater, belt chains, arm warmers (armor off)
            world.getServer().runCommand("item replace entity @p armor.head with minecraft:air");
            world.getServer().runCommand("item replace entity @p armor.chest with minecraft:air");
            world.getServer().runCommand("item replace entity @p armor.legs with minecraft:air");
            world.getServer().runCommand("item replace entity @p armor.feet with minecraft:air");
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EAR_HEADPHONES.get()));
                CosmeticsManager.set(player, FemboySlots.FACE, new ItemStack(FemboyItems.HEART_GLASSES.get()));
                CosmeticsManager.set(player, FemboySlots.OUTFIT_TOP, new ItemStack(FemboyItems.CROP_SWEATER.get()));
                CosmeticsManager.set(player, FemboySlots.WAIST, new ItemStack(FemboyItems.BELT_CHAINS.get()));
                CosmeticsManager.set(player, FemboySlots.HANDS, new ItemStack(FemboyItems.ARM_WARMERS.get()));
                CosmeticsManager.set(player, FemboySlots.OUTFIT_BOTTOM, new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
                ItemStack backpack = new ItemStack(FemboyItems.CANVAS_BACKPACK.get());
                ItemStack badge = new ItemStack(FemboyItems.PRIDE_BADGE.get());
                server.registryAccess().lookupOrThrow(ColorwayPattern.REGISTRY_KEY)
                        .get(ResourceKey.create(ColorwayPattern.REGISTRY_KEY, Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "pride_trans")))
                        .ifPresent(trans -> badge.set(FemboyComponents.COLORWAY.get(), new Colorway(0xFFFFFF, Optional.of(trans), Optional.empty())));
                backpack.set(FemboyComponents.CHARMS.get(), ItemContainerContents.fromItems(List.of(badge)));
                CosmeticsManager.set(player, FemboySlots.BACK, backpack);
            });
            world.getServer().runCommand("tp @p ~ ~ ~ 20 10");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            context.waitTicks(SETTLE_TICKS);
            closeUp(context, "femboymod_closeup_v11_clothing_front");
            for (int color : new int[]{0x2B2A33, 0xF2F0F2}) { // black and white headphone presets
                world.getServer().runOnServer(server -> {
                    ItemStack headphones = new ItemStack(FemboyItems.CAT_EAR_HEADPHONES.get());
                    headphones.set(FemboyComponents.COLORWAY.get(), Colorway.solid(color));
                    CosmeticsManager.set(server.getPlayerList().getPlayers().getFirst(), FemboySlots.HEAD_ACCESSORY, headphones);
                });
                context.waitTicks(SETTLE_TICKS / 4);
                closeUp(context, "femboymod_closeup_headphones_" + Integer.toHexString(color));
            }
            world.getServer().runOnServer(server -> CosmeticsManager.set(server.getPlayerList().getPlayers().getFirst(),
                    FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EAR_HEADPHONES.get())));
            // Dark shades from the front, the fox-style tail from the side and the back
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                CosmeticsManager.set(player, FemboySlots.FACE, new ItemStack(FemboyItems.DARK_SHADES.get()));
                CosmeticsManager.set(player, FemboySlots.TAIL, new ItemStack(FemboyItems.TAIL.get()));
                CosmeticsManager.set(player, FemboySlots.BACK, ItemStack.EMPTY);
            });
            context.waitTicks(SETTLE_TICKS / 4);
            closeUp(context, "femboymod_closeup_dark_shades");
            context.runOnClient(mc -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                if (!mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle();
                }
            });
            world.getServer().runCommand("tp @p ~ ~ ~ 110 40");
            context.waitTicks(SETTLE_TICKS / 2);
            context.takeScreenshot("femboymod_fox_tail_side");
            world.getServer().runCommand("tp @p ~ ~ ~ 180 50");
            context.waitTicks(SETTLE_TICKS / 2);
            context.takeScreenshot("femboymod_fox_tail_back");
            context.runOnClient(mc -> mc.gui.hud.toggle());
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            world.getServer().runCommand("tp @p ~ ~ ~ 20 10");
            world.getServer().runOnServer(server -> CosmeticsManager.set(server.getPlayerList().getPlayers().getFirst(),
                    FemboySlots.FACE, new ItemStack(FemboyItems.HEART_GLASSES.get())));
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            world.getServer().runCommand("tp @p ~ ~ ~ 160 10");
            context.waitTicks(SETTLE_TICKS / 2);
            closeUp(context, "femboymod_closeup_v11_clothing_back");
            context.takeScreenshot("femboymod_v11_back_full");
            world.getServer().runOnServer(server -> CosmeticsManager.set(server.getPlayerList().getPlayers().getFirst(),
                    FemboySlots.HANDS, new ItemStack(FemboyItems.NAIL_POLISH.get())));
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:air");
            world.getServer().runCommand("tp @p ~ ~ ~ 20 10");
            context.waitTicks(SETTLE_TICKS / 2);
            context.takeScreenshot("femboymod_first_person_nails");
            // Emotes (SPEC v1.2): heart hands, wave and peace, seen from the front
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            world.getServer().runCommand("tp @p ~ ~ ~ 20 -10");
            context.runOnClient(mc -> dev.eliasnvx.femboymod.client.EmoteClient.show(mc.player.getId(), dev.eliasnvx.femboymod.emote.Emote.HEART_HANDS));
            context.waitTicks(SETTLE_TICKS / 4);
            closeUp(context, "femboymod_emote_heart_hands");
            context.runOnClient(mc -> dev.eliasnvx.femboymod.client.EmoteClient.show(mc.player.getId(), dev.eliasnvx.femboymod.emote.Emote.WAVE));
            context.waitTicks(SETTLE_TICKS / 4);
            closeUp(context, "femboymod_emote_wave");
            context.runOnClient(mc -> dev.eliasnvx.femboymod.client.EmoteClient.show(mc.player.getId(), dev.eliasnvx.femboymod.emote.Emote.PEACE));
            context.waitTicks(SETTLE_TICKS / 4);
            closeUp(context, "femboymod_emote_peace");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            // Stray cats (SPEC v1.2): five pastel coats
            for (int coat = 0; coat < 5; coat++) {
                world.getServer().runCommand("summon femboymod:stray_cat ~" + (coat - 2) + " ~ ~-3 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f],femboymod_coat:" + coat + "}");
            }
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            world.getServer().runCommand("tp @p ~ ~ ~ 180 35"); // yaw 180 = facing north, toward the cats
            context.waitTicks(SETTLE_TICKS);
            closeUp(context, "femboymod_closeup_stray_cats");
            world.getServer().runCommand("kill @e[type=femboymod:stray_cat]");
            world.getServer().runCommand("summon femboymod:cosplayer ~ ~ ~-3 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f]}");
            world.getServer().runCommand("tp @p ~ ~ ~ 180 5");
            context.waitTicks(SETTLE_TICKS * 2); // let the cats' poof clouds fade
            closeUp(context, "femboymod_closeup_cosplayer");
            world.getServer().runCommand("kill @e[type=femboymod:cosplayer]");
            world.getServer().runCommand("setblock ~ ~ ~-3 femboymod:vibe_scanner[facing=south]");
            context.waitTicks(SETTLE_TICKS * 2);
            closeUp(context, "femboymod_closeup_vibe_scanner");
            world.getServer().runCommand("setblock ~ ~ ~-3 air");
            // Moonstone, neon quartz and rose quartz geode pieces, at night so the glow shows
            world.getServer().runCommand("fill ~-4 ~ ~-7 ~4 ~4 ~-1 air");
            world.getServer().runCommand("fill ~-4 ~ ~-6 ~4 ~3 ~-6 deepslate");
            world.getServer().runCommand("setblock ~-3 ~ ~-5 femboymod:deepslate_moonstone_ore");
            world.getServer().runCommand("setblock ~-3 ~1 ~-5 femboymod:neon_quartz_ore");
            world.getServer().runCommand("fill ~-1 ~ ~-5 ~2 ~ ~-5 femboymod:budding_rose_quartz");
            world.getServer().runCommand("setblock ~-1 ~1 ~-5 femboymod:small_rose_quartz_bud[facing=up]");
            world.getServer().runCommand("setblock ~ ~1 ~-5 femboymod:medium_rose_quartz_bud[facing=up]");
            world.getServer().runCommand("setblock ~1 ~1 ~-5 femboymod:large_rose_quartz_bud[facing=up]");
            world.getServer().runCommand("setblock ~2 ~1 ~-5 femboymod:rose_quartz_cluster[facing=up]");
            world.getServer().runCommand("setblock ~3 ~ ~-5 femboymod:moonstone_lamp");
            for (int design = 0; design < 3; design++) {
                world.getServer().runCommand("setblock ~" + (design - 1) + " ~3 ~-5 femboymod:neon_sign[facing=south,design=" + design + "]");
            }
            world.getServer().runCommand("time set midnight");
            world.getServer().runCommand("tp @p ~ ~ ~ 180 12");
            context.runOnClient(mc -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.fov().set(DEFAULT_FOV);
                if (!mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle();
                }
            });
            context.waitTicks(SETTLE_TICKS * 2);
            context.takeScreenshot("femboymod_new_ores_night");
            world.getServer().runCommand("time set noon");
            context.waitTicks(SETTLE_TICKS);
            context.takeScreenshot("femboymod_new_ores_day");
            context.runOnClient(mc -> mc.gui.hud.toggle());
            world.getServer().runCommand("fill ~-4 ~ ~-7 ~4 ~4 ~-1 air");
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                var patterns = server.registryAccess().lookupOrThrow(ColorwayPattern.REGISTRY_KEY);
                ItemStack pendant = new ItemStack(FemboyItems.MOONSTONE_PENDANT.get());
                ItemStack visor = new ItemStack(FemboyItems.CYBER_VISOR.get());
                patterns.get(ResourceKey.create(ColorwayPattern.REGISTRY_KEY, Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "neon")))
                        .ifPresent(neon -> visor.set(FemboyComponents.COLORWAY.get(), new Colorway(0xFFFFFF, Optional.of(neon), Optional.empty())));
                CosmeticsManager.set(player, FemboySlots.NECK, pendant);
                CosmeticsManager.set(player, FemboySlots.FACE, visor);
            });
            world.getServer().runCommand("tp @p ~ ~ ~ 20 10");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            context.waitTicks(SETTLE_TICKS);
            closeUp(context, "femboymod_closeup_pendant_visor");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            world.getServer().runCommand("tp @p ~ ~ ~ 20 10");
            // Photo Mode: selfie with frame and watermark (saved to screenshots/)
            context.runOnClient(mc -> dev.eliasnvx.femboymod.client.PhotoMode.start());
            context.waitTicks(SETTLE_TICKS * 2);
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));

            // Ears with a visible helmet: they sit on top of it
            world.getServer().runOnServer(server -> CosmeticsManager.set(server.getPlayerList().getPlayers().getFirst(),
                    FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get())));
            world.getServer().runOnServer(server -> CosmeticsManager.setArmorVisibility(server.getPlayerList().getPlayers().getFirst(),
                    net.minecraft.world.entity.EquipmentSlot.HEAD, dev.eliasnvx.femboymod.api.cosmetic.ArmorVisibility.SHOW));
            context.waitTicks(SETTLE_TICKS / 2);
            closeUp(context, "femboymod_closeup_ears_on_helmet");
            world.getServer().runCommand("tp @p ~ ~ ~ 70 10");
            context.waitTicks(SETTLE_TICKS / 2);
            closeUp(context, "femboymod_closeup_ears_on_helmet_side");
            world.getServer().runCommand("tp @p ~ ~ ~ 20 10");
            world.getServer().runOnServer(server -> CosmeticsManager.setArmorVisibility(server.getPlayerList().getPlayers().getFirst(),
                    net.minecraft.world.entity.EquipmentSlot.HEAD, dev.eliasnvx.femboymod.api.cosmetic.ArmorVisibility.AUTO));
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            world.getServer().runCommand("tp @p ~ ~ ~ 0 0");

            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                var pos = player.blockPosition().offset(0, 0, -5);
                if (player.level().getBlockEntity(pos) instanceof dev.eliasnvx.femboymod.block.WardrobeBlockEntity wardrobe) {
                    player.openMenu(wardrobe);
                }
            });
            context.waitTicks(SETTLE_TICKS / 2);
            context.takeScreenshot("femboymod_wardrobe_screen");
            context.runOnClient(mc -> mc.player.closeContainer());
            context.setScreen(() -> new dev.eliasnvx.femboymod.client.ConfigScreen(null));
            context.waitTicks(SETTLE_TICKS / 4);
            context.takeScreenshot("femboymod_config_screen");
            context.setScreen(() -> null);

            // Item icons at a large GUI scale (32x32 icons, SPEC 11.4)
            world.getServer().runOnServer(server -> fillInventoryWithModItems(server.getPlayerList().getPlayers().getFirst()));
            context.waitTicks(SETTLE_TICKS / 4);
            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            context.waitTicks(SETTLE_TICKS / 4);
            context.takeScreenshot("femboymod_inventory_icons");
            context.setScreen(() -> null);
            world.getServer().runOnServer(server -> fillInventoryWithTabVariants(server.getPlayerList().getPlayers().getFirst()));
            context.waitTicks(SETTLE_TICKS / 4);
            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            context.waitTicks(SETTLE_TICKS / 4);
            context.takeScreenshot("femboymod_inventory_colorways");
            context.setScreen(() -> null);
            world.getServer().runOnServer(server -> CosmeticsManager.setHidden(server.getPlayerList().getPlayers().getFirst(),
                    FemboySlots.TAIL, true));
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().openMenu(
                    new net.minecraft.world.SimpleMenuProvider((id, inventory, p) -> new dev.eliasnvx.femboymod.menu.CosmeticsMenu(id, inventory),
                            net.minecraft.network.chat.Component.translatable("container.femboymod.cosmetics"))));
            context.waitTicks(SETTLE_TICKS / 2);
            context.takeScreenshot("femboymod_outfit_screen");
            context.runOnClient(mc -> mc.player.closeContainer());
            world.getServer().runOnServer(server -> CosmeticsManager.setHidden(server.getPlayerList().getPlayers().getFirst(),
                    FemboySlots.TAIL, false));
            world.getServer().runCommand("gamemode creative @p");
            context.waitTicks(SETTLE_TICKS / 4);
            context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(
                    Minecraft.getInstance().player, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS, true));
            context.waitTicks(SETTLE_TICKS / 4);
            context.takeScreenshot("femboymod_creative_panel");
            context.setScreen(() -> null);
            world.getServer().runCommand("gamemode survival @p");
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().getInventory().clearContent());

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
        CosmeticsManager.set(player, FemboySlots.HANDS, new ItemStack(FemboyItems.STRIPED_MITTENS.get()));
    }

    /** Full-HD shot without HUD and toasts, so texture detail can be judged. */
    private static void closeUp(ClientGameTestContext context, String name) {
        context.runOnClient(mc -> {
            mc.options.fov().set(CLOSE_UP_FOV);
            mc.gui.toastManager().clear();
            if (!mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
        });
        context.waitTicks(SETTLE_TICKS / 4);
        context.takeScreenshot(TestScreenshotOptions.of(name).withSize(CLOSE_UP_WIDTH, CLOSE_UP_HEIGHT));
        context.runOnClient(mc -> {
            mc.options.fov().set(ZOOM_FOV);
            mc.gui.hud.toggle();
        });
        context.waitTicks(SETTLE_TICKS / 4);
    }

    private static void fillInventoryWithModItems(ServerPlayer player) {
        player.getInventory().clearContent();
        BuiltInRegistries.ITEM.stream()
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(FemboyMod.MOD_ID))
                .forEach(item -> player.getInventory().add(new ItemStack(item)));
    }

    /** Every colorway preset of the creative tab (socks, hoodies, backpacks), next to the plain item. */
    private static void fillInventoryWithTabVariants(ServerPlayer player) {
        player.getInventory().clearContent();
        CreativeModeTabs.tryRebuildTabContents(FeatureFlags.DEFAULT_FLAGS, true, player.level().registryAccess());
        Set<Item> withVariants = FemboyItems.TAB.get().getDisplayItems().stream()
                .filter(stack -> stack.has(FemboyComponents.COLORWAY.get()))
                .map(ItemStack::getItem)
                .collect(Collectors.toSet());
        FemboyItems.TAB.get().getDisplayItems().stream()
                .filter(stack -> withVariants.contains(stack.getItem()))
                .forEach(stack -> player.getInventory().add(stack.copy()));
    }
}
