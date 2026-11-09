package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.energy.FemboyEffects;
import dev.eliasnvx.femboymod.entity.FemboyEntities;
import dev.eliasnvx.femboymod.entity.RubberDuck;
import dev.eliasnvx.femboymod.entity.SockGoblin;
import dev.eliasnvx.femboymod.pet.PetOutfitItem;
import dev.eliasnvx.femboymod.pet.PetOutfits;
import dev.eliasnvx.femboymod.block.DebugFlytrapBlock;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.entity.CatKnockGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compat screenshots for 1.20.1, which has no client GameTest API. Only with {@code -Dfemboymod.compat_harness=true}
 * (the {@code runCompatClient} task): creates a flat creative world, dresses the player, shoots them from the front,
 * walking and from behind, leaves and loads the world again and shoots them once more, then quits. With Figura in the runtime ({@code -Pcompat_figura=true}) it then loads two local
 * test avatars and shoots each: one that keeps the vanilla model, one that hides it like most custom avatars do.
 * Files: {@code fabric/build/run/compatClient/screenshots/compat_*.png}. Dev-only test code (Figura by reflection).
 * {@code -Pharness_scene=pets|halloween|mobs|interior|mirror|ears|slide|cats|plants|blocks|settings} shoots another scene
 * instead (pet outfits on adults, kittens and puppies, plus a real headpat; Halloween costumes; Sock Goblin and Rubber Duck; the
 * furnished room; the mirror and the vanity table, and a pet beside the mirror with and without a wall between; reactive ears
 * at a creeper; sock sliding; a cat knocking a pot off a table; the 0.3.0 plants; every block with see-through texture parts
 * next to some solid ones; the settings screen), as on 1.21.1 plus the mirror's pet shots and the settings scene.
 */
public final class CompatHarness implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("femboymod");
    private static final String PROPERTY = "femboymod.compat_harness";
    /** Chunks and the player model settle after joining. */
    private static final int JOIN_TICKS = 60;
    private static final int SETTLE = 20;
    private static final int WALK_TICKS = 10;
    /** Figura loads avatars off thread. */
    private static final int AVATAR_TICKS = 60;
    private static final int FOV = 50;

    /** One thing to do, then how many ticks to wait before the next. */
    private record Step(String name, Consumer<Minecraft> action, int ticksAfter) {
    }

    private static final String SCENE_PROPERTY = "femboymod.harness_scene";
    /** Pets scene: the row (kitten and puppy second and fourth), how far ahead and apart, close-up distance. */
    private static final EntityType<? extends TamableAnimal>[] PETS = petTypes();
    private static final boolean[] BABIES = {false, true, false, true, false};
    private static final int KITTEN = 1;
    private static final int PUPPY = 3;
    private static final int ROW_DISTANCE = 3;
    private static final int PET_GAP = 2;
    private static final int ROW_BACK = 2;
    private static final double CLOSE = 1.6;
    private static final float CLOSE_PITCH = 30.0F;
    /** Standing this far behind the kitten and looking this far down puts the crosshair on it while sneaking. */
    private static final double PAT_DISTANCE = 1.2;
    private static final float PAT_PITCH = 45.0F;
    /** The pat emote and the hearts are short: shoot while they play. */
    private static final int PAT_TICKS = 4;
    /** Interior scene: pets look for a bed every 2 s and need time to walk over and lie down. */
    private static final int PET_BED_TICKS = 400;
    private static final int INTERIOR_FOV = 70;
    @SuppressWarnings("unchecked")
    private static final java.util.function.Supplier<Item>[] HEADS = new java.util.function.Supplier[]{
            FemboyItems.PET_BOW, FemboyItems.MINI_WITCH_HAT};
    @SuppressWarnings("unchecked")
    private static final java.util.function.Supplier<Item>[] NECKS = new java.util.function.Supplier[]{
            FemboyItems.BELL_COLLAR, FemboyItems.PET_BANDANA, FemboyItems.BELL_COLLAR};

    /** Ears scene: the creeper stands this far west (the player's right when facing south) and south of the player. */
    private static final int EARS_CREEPER_DX = -5;
    private static final int EARS_CREEPER_DZ = 1;
    /** Two thirds of a creeper's 30-tick fuse: swollen enough to flatten the ears, removed well before it explodes. */
    private static final int EARS_SWELL_TICKS = 20;

    /** Slide scene (26.3's SockSlideClientTest): a plank floor this long ahead, run, then coast without input. */
    private static final int SLIDE_FLOOR = 24;
    private static final int SLIDE_RUN_TICKS = 25;
    private static final int SLIDE_COAST_TICKS = 30;
    /** The shot is taken this long after letting go, mid-slide. */
    private static final int SLIDE_SHOT_TICKS = 2;
    private static final double CARPET_HEIGHT = 1.0 / 16;
    /** Socks must coast at least this many times further than sneakers; the carpet no further than that. */
    private static final double SOCKS_VS_SHOES = 1.5;
    private static final String[] SLIDE_RUNS = {"socks", "sneakers", "carpet"};
    private static final ResourceLocation DRIFT = new ResourceLocation("femboymod", "main/sock_drift");

    /**
     * Cats scene (26.3's CatsClientTest): a quick, certain knock goal (the cat's own one checks every 10 s with a
     * chance), driven on the server once per client tick; how long to wait for the walk and how long after the push.
     */
    private static final CommonConfig.Cats CATS_EAGER = new CommonConfig.Cats(20, 1.0F, 6000, 6, 12.0, 30);
    private static final int CATS_MAX_TICKS = 300;
    private static final int CATS_AFTER_KNOCK = 25;
    private static final double CATS_CLOSE = 1.5;
    private static final int CATS_FOV = 60;
    /** Plants scene: the bug waits next to the seedling; the first check comes 10 ticks after placing. */
    private static final int PLANTS_SNACK_TICKS = 40;

    /**
     * Blocks scene: every block whose model has transparent texture pixels (and a few solid ones to compare), in rows
     * of up to five; one row per station, stations this far apart along x, each shot from two blocks back.
     */
    private static final String[][] BLOCK_STATIONS = {
            {"ores", "rose_quartz_ore", "deepslate_rose_quartz_ore", "deepslate_moonstone_ore", "glitter_ore", "neon_quartz_ore"},
            {"quartz", "small_rose_quartz_bud[facing=up]", "medium_rose_quartz_bud[facing=up]", "large_rose_quartz_bud[facing=up]",
                    "rose_quartz_cluster[facing=up]", "budding_rose_quartz"},
            {"signs", "neon_sign[design=0,facing=south]", "neon_sign[design=1,facing=south]", "neon_sign[design=2,facing=south]",
                    "led_strip[facing=up,color=3]", "lava_lamp[color=pink,lit=true]"},
            {"soft", "bean_bag[facing=south,color=pink]", "pet_bed[facing=south,color=purple]", "cat_plush[facing=south]",
                    "creeper_plush[facing=south]", "shark_plush[facing=south]"},
            {"gamer", "rubber_duck[facing=south]", "gamer_chair[facing=south]", "gamer_keyboard[facing=south]",
                    "gamer_monitor[facing=south,lit=true]", "terminal[facing=south]"},
            {"home", "laundry_basket[facing=south,open=false]", "mini_fridge[facing=south,open=false]", "moonstone_lamp[lit=true]",
                    "vanity_table[facing=south,half=lower]", "vibe_scanner[facing=south]"}};
    private static final int BLOCK_STATION_GAP = 8;
    private static final int BLOCK_ROW = -2;
    /** The second shot of a station stands this far back (z, from the scene's origin) to fit the whole row. */
    private static final double BLOCK_WIDE_BACK = 2.0;

    private final Deque<Step> steps = new ArrayDeque<>();
    /** Runs on the server once per client tick while set (the cats scene drives its knock goal with it). */
    private volatile Runnable serverDriver;
    /** Cats scene, written on the server thread: ticks the cat spent next to the pot, and whether the pot is gone. */
    private volatile int catCloseTicks;
    private volatile boolean potGone;
    private volatile Cat catsCat;
    private BlockPos catPot = BlockPos.ZERO;
    private net.minecraft.world.entity.monster.Creeper earsCreeper;
    /** Slide scene: where the player let go, and how far each run coasted (in {@link #SLIDE_RUNS} order). */
    private Vec3 released = Vec3.ZERO;
    private final double[] coasts = new double[SLIDE_RUNS.length];
    private final TamableAnimal[] pets = new TamableAnimal[PETS.length];
    private BlockPos origin = BlockPos.ZERO;
    private boolean worldCreated;
    private boolean joined;
    private int wait;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean(PROPERTY)) {
            return;
        }
        plan();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void plan() {
        step("setup", mc -> {
            command(mc, "time set noon");
            command(mc, "weather clear");
            command(mc, "gamerule doDaylightCycle false");
            command(mc, "gamerule doWeatherCycle false");
            command(mc, "execute as @p at @s run tp @s ~ ~ ~ 0 0");
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.options.fov().set(FOV);
            mc.options.hideGui = true;
            mc.options.pauseOnLostFocus = false; // the window is in the background while the harness runs
        }, SETTLE);
        switch (System.getProperty(SCENE_PROPERTY, "compat")) {
            case "pets" -> planPets();
            case "halloween" -> planHalloween();
            case "mobs" -> planMobs();
            case "interior" -> planInterior();
            case "mirror" -> planMirror();
            case "ears" -> planEars();
            case "slide" -> planSlide();
            case "cats" -> planCats();
            case "plants" -> planPlants();
            case "blocks" -> planBlocks();
            case "settings" -> planSettings();
            default -> planCompat();
        }
        step("quit", Minecraft::stop, 0);
    }

    /** Cosmetics next to other mods: the outfit, a rejoin, Figura avatars (when Figura is installed). */
    private void planCompat() {
        step("dress", CompatHarness::dress, SETTLE * 2);
        shots("compat_vanilla_outfit");
        // leave and load the world again: the outfit was saved and has to come back on join
        step("rejoin", mc -> {
            mc.level.disconnect();
            mc.clearLevel(new TitleScreen());
            joined = false;
            mc.createWorldOpenFlows().loadLevel(new TitleScreen(), WORLD);
        }, 0);
        step("check rejoined outfit", mc -> {
            if (CosmeticsManager.get(mc.player).items().isEmpty()) {
                throw new IllegalStateException("the client lost its own outfit when joining again");
            }
        }, 0);
        shots("compat_rejoined");
        if (FabricLoader.getInstance().isModLoaded("figura")) {
            avatar("keep_vanilla", "-- keeps the vanilla player model; cosmetics should look as without Figura\n");
            avatar("hide_vanilla", "-- hides the vanilla player model, as avatars with their own model do\n"
                    + "vanilla_model.PLAYER:setVisible(false)\n");
        }
    }

    /**
     * Pet outfits in a row in front of the player (adult cat, kitten, adult wolf, puppy, parrot; all sitting, facing the
     * player), then the kitten and the puppy up close from the front and the side, then with the other pieces, then a
     * real headpat on the kitten (sneak + right-click), checked on the server.
     */
    private void planPets() {
        step("pets", mc -> {
            origin = mc.player.blockPosition();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                for (int i = 0; i < PETS.length; i++) {
                    pets[i] = pet(player, PETS[i], BABIES[i], (i - PETS.length / 2) * PET_GAP);
                    dress(player, pets[i], HEADS[i % HEADS.length].get(), NECKS[i % NECKS.length].get());
                }
            });
        }, SETTLE * 2);
        step("row", mc -> tpPlayer(mc, origin.getX() + 0.5, origin.getZ() + 0.5 - ROW_BACK, 0.0F), SETTLE);
        step("pets_row", mc -> shot(mc, "pets_row"), 1);
        closeUps("pets");
        step("swap", mc -> {
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                for (int i = 0; i < PETS.length; i++) {
                    dress(player, pets[i], HEADS[(i + 1) % HEADS.length].get(), NECKS[(i + 1) % NECKS.length].get());
                }
            });
        }, SETTLE);
        closeUps("pets_swapped");
        // a real headpat on the kitten: stand behind it, look down at it, sneak and right-click with an empty hand
        step("headpat", mc -> {
            command(mc, "clear @p"); // the swap put the old pieces in the player's hands
            tpPlayer(mc, petX(KITTEN), petZ() - PAT_DISTANCE, 0.0F, PAT_PITCH);
        }, SETTLE / 2);
        step("sneak", mc -> mc.options.keyShift.setDown(true), SETTLE / 2);
        step("pat", mc -> KeyMapping.click(KeyBindingHelper.getBoundKeyOf(mc.options.keyUse)), PAT_TICKS);
        step("pets_headpat_first_person", mc -> {
            shot(mc, "pets_headpat_first_person");
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }, 1);
        step("pets_headpat_third_person", mc -> {
            shot(mc, "pets_headpat_third_person");
            mc.options.keyShift.setDown(false);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }, 1);
        step("check headpat", mc -> {
            String problem = mc.getSingleplayerServer().submit(() -> {
                TamableAnimal kitten = pets[KITTEN];
                if (!kitten.hasEffect(FemboyEffects.holder(FemboyEffects.COZY))) {
                    return "the kitten is not Cozy after the headpat";
                }
                return kitten.isOrderedToSit() ? null : "the headpat also made the kitten stand up";
            }).join();
            if (problem != null) {
                throw new IllegalStateException(problem);
            }
        }, 0);
    }

    /** The kitten and the puppy from the front and from their right side. */
    private void closeUps(String name) {
        for (int index : new int[]{KITTEN, PUPPY}) {
            String which = index == KITTEN ? "kitten" : "puppy";
            step(name + "_" + which + "_front", mc -> tpPlayer(mc, petX(index), petZ() - CLOSE, 0.0F), SETTLE);
            step("shot", mc -> shot(mc, name + "_" + which + "_front"), 1);
            step(name + "_" + which + "_side", mc -> tpPlayer(mc, petX(index) - CLOSE, petZ(), -90.0F), SETTLE);
            step("shot", mc -> shot(mc, name + "_" + which + "_side"), 1);
        }
    }

    private double petX(int index) {
        return origin.getX() + 0.5 + (index - PETS.length / 2) * PET_GAP;
    }

    private double petZ() {
        return origin.getZ() + 0.5 + ROW_DISTANCE;
    }

    /**
     * The 0.2.0 mobs standing still in front of the player: a Sock Goblin holding stolen socks, a Rubber Duck on the
     * grass and one floating in a pool, then each up close.
     */
    private void planMobs() {
        step("mobs", mc -> {
            origin = mc.player.blockPosition();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            command(mc, "execute as @p at @s run fill ~1 ~-1 ~3 ~3 ~-1 ~5 minecraft:water");
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                SockGoblin goblin = still(player, FemboyEntities.SOCK_GOBLIN.get(), -2, ROW_DISTANCE);
                goblin.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
                still(player, FemboyEntities.RUBBER_DUCK.get(), 0, ROW_DISTANCE);
                RubberDuck floating = FemboyEntities.RUBBER_DUCK.get().create(player.level());
                floating.moveTo(origin.getX() + 2.5, origin.getY(), origin.getZ() + ROW_DISTANCE + 1.5, 180.0F, 0.0F);
                player.level().addFreshEntity(floating);
            });
        }, SETTLE * 4);
        step("mobs_row", mc -> shot(mc, "mobs_row"), 1);
        step("goblin_close", mc -> tpPlayer(mc, origin.getX() - 1.5, origin.getZ() + ROW_DISTANCE - CLOSE - 0.5, 0.0F), SETTLE);
        step("mobs_goblin", mc -> shot(mc, "mobs_goblin"), 1);
        step("goblin_side", mc -> tpPlayer(mc, origin.getX() - 1.5 - CLOSE - 0.5, origin.getZ() + ROW_DISTANCE + 0.5, -90.0F), SETTLE);
        step("mobs_goblin_side", mc -> shot(mc, "mobs_goblin_side"), 1);
        step("duck_close", mc -> tpPlayer(mc, origin.getX() + 0.5, origin.getZ() + ROW_DISTANCE - CLOSE + 0.5, 0.0F), SETTLE);
        step("mobs_duck", mc -> shot(mc, "mobs_duck"), 1);
        step("pool_side", mc -> tpPlayer(mc, origin.getX() + 2.5 - CLOSE - 1.0, origin.getZ() + ROW_DISTANCE + 1.5, -90.0F), SETTLE);
        step("mobs_duck_floating", mc -> shot(mc, "mobs_duck_floating"), 1);
    }

    /**
     * A furnished room with the interior pieces (same layout as 26.3's InteriorClientTest), by day and by night, a cat and
     * a dog on the pet beds, the vanity mirror up close, the open fridge and basket.
     */
    private void planInterior() {
        step("room", mc -> {
            origin = mc.player.blockPosition();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.fov().set(INTERIOR_FOV);
            room(mc, "tp @s ~0.5 ~ ~0.5 180 22");
            room(mc, "fill ~-4 ~-1 ~-1 ~4 ~-1 ~-8 minecraft:oak_planks");
            room(mc, "fill ~-4 ~ ~-8 ~4 ~3 ~-8 minecraft:white_concrete");
            room(mc, "fill ~-4 ~ ~-1 ~-4 ~3 ~-8 minecraft:white_concrete");
            room(mc, "fill ~4 ~ ~-1 ~4 ~3 ~-8 minecraft:white_concrete");
            room(mc, "setblock ~-2 ~ ~-7 femboymod:vanity_table[facing=south,half=lower]");
            room(mc, "setblock ~-2 ~1 ~-7 femboymod:vanity_table[facing=south,half=upper]");
            room(mc, "setblock ~-3 ~ ~-5 femboymod:mini_fridge[facing=east,open=true]");
            room(mc, "setblock ~-3 ~ ~-3 femboymod:laundry_basket[facing=east,open=true]");
            room(mc, "setblock ~2 ~ ~-7 femboymod:bean_bag[facing=south,color=pink]");
            room(mc, "setblock ~3 ~ ~-7 femboymod:lava_lamp[color=purple,lit=true]");
            room(mc, "setblock ~0 ~ ~-7 femboymod:lava_lamp[color=light_blue,lit=true]");
            for (int x = -1; x <= 3; x++) {
                room(mc, "setblock ~" + x + " ~2 ~-7 femboymod:fairy_lights[facing=south,mode=2]");
            }
            room(mc, "setblock ~0 ~ ~-4 femboymod:fluffy_rug[shape=heart,color=light_blue]");
            room(mc, "setblock ~1 ~ ~-4 femboymod:fluffy_rug[shape=round,color=white]");
            room(mc, "setblock ~-1 ~ ~-4 femboymod:fluffy_rug[shape=square,color=pink]");
            room(mc, "setblock ~2 ~ ~-3 femboymod:pet_bed[facing=west,color=purple]");
            room(mc, "setblock ~2 ~ ~-5 femboymod:pet_bed[facing=west,color=light_blue]");
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                for (EntityType<? extends TamableAnimal> type : List.of(EntityType.CAT, EntityType.WOLF)) {
                    TamableAnimal pet = type.create(player.level());
                    if (pet == null) {
                        throw new IllegalStateException("could not create " + type);
                    }
                    BlockPos pos = origin.offset(type == EntityType.CAT ? 1 : -1, 0, -2);
                    pet.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    pet.tame(player);
                    player.level().addFreshEntity(pet);
                }
            });
        }, PET_BED_TICKS);
        step("interior_day", mc -> shot(mc, "interior_day"), 1);
        step("night", mc -> command(mc, "time set midnight"), SETTLE);
        step("interior_night", mc -> shot(mc, "interior_night"), 1);
        step("pets", mc -> {
            command(mc, "time set noon");
            room(mc, "tp @s ~-0.5 ~ ~-3.5 -90 35");
        }, SETTLE);
        step("interior_pets", mc -> shot(mc, "interior_pets"), 1);
        step("vanity", mc -> room(mc, "tp @s ~-1.5 ~ ~-4.5 180 15"), SETTLE);
        step("interior_vanity", mc -> shot(mc, "interior_vanity"), 1);
        step("storage", mc -> room(mc, "tp @s ~0.5 ~ ~-0.5 120 30"), SETTLE);
        step("interior_storage", mc -> shot(mc, "interior_storage"), 1);
        step("lamps", mc -> room(mc, "tp @s ~1.5 ~ ~-4.5 180 10"), SETTLE);
        step("interior_lamps", mc -> shot(mc, "interior_lamps"), 1);
    }

    /**
     * A standing mirror 4 blocks ahead at midnight, then noon: the dressed player far and near, in first person and
     * from behind; then the vanity table (same layout as 26.3's MirrorClientTest).
     */
    private void planMirror() {
        step("mirror", mc -> {
            origin = mc.player.blockPosition();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            command(mc, "time set midnight");
            room(mc, "tp @s ~0.5 ~ ~0.5 0 10");
            room(mc, "setblock ~ ~ ~4 femboymod:mirror[facing=north,half=lower]");
            room(mc, "setblock ~ ~1 ~4 femboymod:mirror[facing=north,half=upper]");
            room(mc, "setblock ~4 ~ ~4 femboymod:vanity_table[facing=north,half=lower]");
            room(mc, "setblock ~4 ~1 ~4 femboymod:vanity_table[facing=north,half=upper]");
            dress(mc);
        }, SETTLE * 2);
        step("mirror_far", mc -> shot(mc, "mirror_far"), 1);
        step("near", mc -> room(mc, "tp @s ~0.5 ~ ~2.9 0 10"), SETTLE);
        step("mirror_near", mc -> shot(mc, "mirror_near"), 1);
        step("behind", mc -> {
            room(mc, "tp @s ~0.5 ~ ~1.5 0 10");
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }, SETTLE);
        step("mirror_behind", mc -> shot(mc, "mirror_behind"), 1);
        step("noon", mc -> {
            command(mc, "time set noon");
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            room(mc, "tp @s ~0.5 ~ ~2.4 0 10");
        }, SETTLE);
        step("mirror_noon", mc -> shot(mc, "mirror_noon"), 1);
        step("vanity", mc -> room(mc, "tp @s ~4.5 ~ ~2.4 0 15"), SETTLE);
        step("mirror_vanity", mc -> shot(mc, "mirror_vanity"), 1);
        // a tamed cat beside the mirror is reflected; with a wall between it and the mirror it isn't (no seeing into the
        // next room). Seen from the left, so the cat's reflection is in the glass (26.3's MirrorClientTest)
        step("pet", mc -> {
            room(mc, "tp @s ~-2.5 ~ ~1.5 -50 10");
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                Cat cat = EntityType.CAT.create(player.level());
                if (cat == null) {
                    throw new IllegalStateException("could not create a cat");
                }
                cat.moveTo(origin.getX() + 2.5, origin.getY(), origin.getZ() + 2.5, 0.0F, 0.0F);
                cat.setNoAi(true);
                cat.tame(player); // mirrors show pets once tamed
                player.level().addFreshEntity(cat);
            });
        }, SETTLE);
        step("mirror_pet_in_sight", mc -> shot(mc, "mirror_pet_in_sight"), 1);
        step("wall", mc -> room(mc, "fill ~1 ~ ~3 ~1 ~1 ~3 minecraft:stone"), SETTLE);
        step("mirror_pet_behind_wall", mc -> shot(mc, "mirror_pet_behind_wall"), 1);
    }

    /**
     * The settings screen, a scrolling list since 0.3.0 (the old layout cut off the last options and Done with a large GUI
     * scale): the top, then scrolled to the end, where the headpat option and Done are.
     */
    private void planSettings() {
        step("settings", mc -> mc.setScreen(new dev.eliasnvx.femboymod.client.ConfigScreen(null)), SETTLE);
        step("settings_top", mc -> shot(mc, "settings_top"), 1);
        step("scroll", mc -> mc.screen.mouseScrolled(mc.screen.width / 2.0, mc.screen.height / 2.0, -20.0), SETTLE / 2);
        step("settings_end", mc -> shot(mc, "settings_end"), 1);
        step("close", mc -> mc.setScreen(null), 1);
    }

    /**
     * Reactive ears and tail: cat ears and a tail, first with nothing around, then a still creeper on the player's right
     * (the ears turn toward it; cat ears notice creepers first), from the front and behind, the ear tips in first person
     * (HUD shown), and the creeper swelling (ears flatten, the tail puffs up). Same idea as 26.3's EarsClientTest.
     */
    private void planEars() {
        step("ears", mc -> {
            origin = mc.player.blockPosition();
            command(mc, "difficulty easy"); // peaceful removes the creeper
            command(mc, "gamerule doMobSpawning false");
            command(mc, "gamerule mobGriefing false");
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get()));
                CosmeticsManager.set(player, FemboySlots.TAIL, new ItemStack(FemboyItems.TAIL.get()));
            });
        }, SETTLE * 2);
        step("ears_rest", mc -> shot(mc, "ears_rest"), 1);
        step("creeper", mc -> {
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> earsCreeper = still(server.getPlayerList().getPlayers().get(0), EntityType.CREEPER,
                    EARS_CREEPER_DX, EARS_CREEPER_DZ));
        }, SETTLE * 2);
        step("ears_notice", mc -> {
            shot(mc, "ears_notice");
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }, SETTLE / 2);
        step("ears_notice_back", mc -> {
            shot(mc, "ears_notice_back");
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = false; // the tips are a HUD layer
            mc.getToasts().clear(); // the tutorial toast covers the top right corner
            mc.gui.getChat().clearMessages(false);
        }, SETTLE);
        step("ears_tips_first_person", mc -> {
            shot(mc, "ears_tips_first_person");
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }, SETTLE / 2);
        step("hiss", mc -> mc.getSingleplayerServer().execute(() -> earsCreeper.setSwellDir(1)), EARS_SWELL_TICKS);
        step("ears_creeper_hiss", mc -> {
            shot(mc, "ears_creeper_hiss");
            mc.getSingleplayerServer().execute(() -> earsCreeper.discard()); // before it explodes
        }, SETTLE);
    }

    /**
     * Sock sliding, measured like 26.3's SockSlideClientTest (players move on their own client): run south along a plank
     * floor, let go and log how far the player keeps going in socks, in sneakers and in socks on a carpet. Socks must
     * coast clearly further, the carpet must grip, and the coast must earn Parquet Drift on the server.
     */
    private void planSlide() {
        step("slide", mc -> {
            origin = mc.player.blockPosition();
            command(mc, "gamemode survival @p");
            mc.mouseHandler.releaseMouse(); // a moving mouse would steer the runs off the floor
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            room(mc, "fill ~-3 ~-1 ~-2 ~3 ~-1 ~" + SLIDE_FLOOR + " minecraft:oak_planks");
            room(mc, "fill ~-3 ~ ~-2 ~3 ~3 ~" + SLIDE_FLOOR + " minecraft:air");
        }, SETTLE);
        coast(0, FemboyItems.PROGRAMMING_SOCKS.get(), null, 0);
        coast(1, FemboyItems.PROGRAMMING_SOCKS.get(), FemboyItems.SNEAKERS.get(), 0);
        step("carpet", mc -> room(mc, "fill ~-3 ~ ~-2 ~3 ~ ~" + SLIDE_FLOOR + " minecraft:moss_carpet"), 1);
        coast(2, FemboyItems.PROGRAMMING_SOCKS.get(), null, CARPET_HEIGHT);
        step("check slide", mc -> {
            LOGGER.info("compat harness: sock slide coast: socks {} blocks, sneakers {}, socks on carpet {}",
                    coasts[0], coasts[1], coasts[2]);
            if (coasts[0] < coasts[1] * SOCKS_VS_SHOES) {
                throw new IllegalStateException("socks coasted " + coasts[0] + " blocks, sneakers " + coasts[1]);
            }
            if (coasts[2] > coasts[1] * SOCKS_VS_SHOES) {
                throw new IllegalStateException("the carpet didn't grip: " + coasts[2] + " blocks, sneakers " + coasts[1]);
            }
            IntegratedServer server = mc.getSingleplayerServer();
            boolean drifted = server.submit(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                Advancement drift = server.getAdvancements().getAdvancement(DRIFT);
                return drift != null && player.getAdvancements().getOrStartProgress(drift).isDone();
            }).join();
            if (!drifted) {
                throw new IllegalStateException("coasting in socks didn't earn " + DRIFT);
            }
        }, 0);
    }

    /** Dresses the player, puts them at the start ({@code lift} above the floor), runs, lets go and measures the coast. */
    private void coast(int run, Item legs, Item feet, double lift) {
        String name = SLIDE_RUNS[run];
        step("wear " + name, mc -> {
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(legs));
                CosmeticsManager.set(player, FemboySlots.FEET, feet == null ? ItemStack.EMPTY : new ItemStack(feet));
            });
            command(mc, String.format(java.util.Locale.ROOT, "tp @p %.3f %.4f %.3f 0 0",
                    origin.getX() + 0.5, origin.getY() + lift, origin.getZ() + 0.5));
        }, SETTLE);
        step("run " + name, mc -> mc.options.keyUp.setDown(true), SLIDE_RUN_TICKS);
        step("let go " + name, mc -> {
            mc.options.keyUp.setDown(false);
            released = mc.player.position();
        }, SLIDE_SHOT_TICKS);
        step("slide_" + name, mc -> shot(mc, "slide_" + name), SLIDE_COAST_TICKS - SLIDE_SHOT_TICKS);
        step("stopped " + name, mc -> coasts[run] = mc.player.position().subtract(released).horizontalDistance(), 0);
    }

    /**
     * Cats knocking things off tables (same layout as 26.3's CatsClientTest): the player looks north at a two-block table
     * with a potted poppy on its east end, a tamed cat sits on the floor west of it. Shots before, during the stare and
     * after the push; logs whether the pot came down as its items (a flower pot and a poppy).
     */
    private void planCats() {
        step("cats", mc -> {
            origin = mc.player.blockPosition();
            catPot = origin.offset(0, 1, -4);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.fov().set(CATS_FOV);
            command(mc, "gamerule doMobSpawning false");
            room(mc, "tp @s ~0.5 ~ ~0.5 180 20");
            room(mc, "fill ~-6 ~-1 ~-8 ~6 ~-1 ~1 minecraft:oak_planks");
            room(mc, "fill ~-6 ~ ~-8 ~6 ~4 ~1 minecraft:air");
            room(mc, "setblock ~-1 ~ ~-4 minecraft:spruce_planks");
            room(mc, "setblock ~ ~ ~-4 minecraft:spruce_planks");
            room(mc, "setblock ~ ~1 ~-4 minecraft:potted_poppy");
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                Cat cat = EntityType.CAT.create(player.level());
                if (cat == null) {
                    throw new IllegalStateException("could not create a cat");
                }
                cat.moveTo(origin.getX() - 2.5, origin.getY(), origin.getZ() - 3.5, 90.0F, 0.0F);
                cat.tame(player);
                cat.removeFreeWill(); // only the knock goal below moves it, no strolling or following in between
                player.level().addFreshEntity(cat);
                catsCat = cat;
            });
        }, SETTLE * 2);
        step("cats_before", mc -> shot(mc, "cats_before"), 1);
        step("cats go", mc -> serverDriver = catDriver(catsCat, new CatKnockGoal(catsCat, () -> CATS_EAGER)), 0);
        // the stare: shoot two thirds into it
        until("cats stare", CATS_MAX_TICKS, mc -> catCloseTicks >= CATS_EAGER.knockStareTicks() * 2 / 3,
                mc -> shot(mc, "cats_stare"));
        until("cats push", CATS_MAX_TICKS, mc -> potGone, mc -> serverDriver = null);
        step("cats settle", mc -> { }, CATS_AFTER_KNOCK);
        step("cats_knocked", mc -> shot(mc, "cats_knocked"), 1);
        step("check cats", mc -> {
            IntegratedServer server = mc.getSingleplayerServer();
            int[] items = server.submit(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                List<ItemEntity> drops = player.level().getEntitiesOfClass(ItemEntity.class, new AABB(catPot).inflate(4));
                int pots = 0;
                int poppies = 0;
                for (ItemEntity drop : drops) {
                    if (drop.getItem().is(Items.FLOWER_POT)) {
                        pots += drop.getItem().getCount();
                    } else if (drop.getItem().is(Items.POPPY)) {
                        poppies += drop.getItem().getCount();
                    }
                }
                return new int[]{player.level().getBlockState(catPot).isAir() ? 1 : 0, pots, poppies, drops.size()};
            }).join();
            LOGGER.info("compat harness: cat mischief: pot knocked off {}, on the floor {} flower pot(s), {} poppy(ies), {} item stack(s)",
                    items[0] == 1, items[1], items[2], items[3]);
            if (items[0] != 1 || items[1] != 1 || items[2] != 1) {
                throw new IllegalStateException("the cat didn't knock the pot off as one flower pot and one poppy");
            }
        }, 0);
    }

    /** Ticks a knock goal like the goal selector would, and notes how close the cat is and whether the pot is gone. */
    private Runnable catDriver(Cat cat, CatKnockGoal goal) {
        boolean[] running = {false};
        Vec3 pot = Vec3.atBottomCenterOf(catPot);
        return () -> {
            if (!running[0]) {
                running[0] = goal.canUse();
                if (running[0]) {
                    goal.start();
                }
            } else if (goal.canContinueToUse()) {
                goal.tick();
            } else {
                goal.stop();
                running[0] = false;
            }
            if (cat.position().distanceTo(pot) < CATS_CLOSE) {
                catCloseTicks++;
            }
            potGone = cat.level().getBlockState(catPot).isAir();
        };
    }

    /**
     * Render layers (1.20.1 sets them per block, 26.3 picks them from the textures): each station's row of blocks on a
     * plank floor in daylight, a stone wall behind, shot from two blocks back and again from further back. Transparent
     * texture pixels drawn as solid show up black (ore overlays, buds, neon signs).
     */
    private void planBlocks() {
        step("blocks", mc -> {
            origin = mc.player.blockPosition();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.fov().set(INTERIOR_FOV);
            int width = BLOCK_STATIONS.length * BLOCK_STATION_GAP;
            room(mc, "fill ~-4 ~-1 ~-4 ~" + width + " ~-1 ~2 minecraft:oak_planks");
            room(mc, "fill ~-4 ~ ~-4 ~" + width + " ~4 ~2 minecraft:air");
            room(mc, "fill ~-4 ~ ~-4 ~" + width + " ~2 ~-4 minecraft:stone_bricks");
            for (int station = 0; station < BLOCK_STATIONS.length; station++) {
                String[] row = BLOCK_STATIONS[station];
                for (int i = 1; i < row.length; i++) {
                    int x = station * BLOCK_STATION_GAP + i - 3;
                    room(mc, "setblock ~" + x + " ~ ~" + BLOCK_ROW + " femboymod:" + row[i]);
                    if (row[i].startsWith("vanity_table")) {
                        room(mc, "setblock ~" + x + " ~1 ~" + BLOCK_ROW + " femboymod:vanity_table[facing=south,half=upper]");
                    }
                }
            }
        }, SETTLE * 2);
        for (int station = 0; station < BLOCK_STATIONS.length; station++) {
            String name = "blocks_" + BLOCK_STATIONS[station][0];
            int x = station * BLOCK_STATION_GAP;
            step(name, mc -> tpPlayer(mc, origin.getX() + x + 0.5, origin.getZ() + 0.5, 180.0F, 25.0F), SETTLE);
            step("shot", mc -> shot(mc, name), 1);
            step(name + "_wide", mc -> tpPlayer(mc, origin.getX() + x + 0.5, origin.getZ() + BLOCK_WIDE_BACK, 180.0F, 20.0F), SETTLE);
            step("shot", mc -> shot(mc, name + "_wide"), 1);
        }
    }

    /**
     * The 0.3.0 plants on a lawn (26.3's PlantsClientTest idea): the Debug Flytrap's three stages, a shelf with the
     * potted flytrap, potted catnip and Potted Grass, a catnip patch; then a still Bug next to the seedling: a flytrap
     * must eat it and show its closed jaws.
     */
    private void planPlants() {
        step("plants", mc -> {
            origin = mc.player.blockPosition();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.fov().set(INTERIOR_FOV);
            command(mc, "difficulty easy"); // the Bug is a monster: peaceful would remove it
            command(mc, "gamerule doMobSpawning false");
            room(mc, "tp @s ~0.5 ~ ~0.5 180 25");
            room(mc, "fill ~-6 ~-1 ~-8 ~6 ~-1 ~1 minecraft:grass_block");
            room(mc, "fill ~-6 ~ ~-8 ~6 ~4 ~1 minecraft:air");
            for (int stage = 0; stage <= DebugFlytrapBlock.MAX_STAGE; stage++) {
                room(mc, "setblock ~" + (stage - 3) + " ~ ~-3 femboymod:debug_flytrap[stage=" + stage + "]");
            }
            room(mc, "fill ~1 ~ ~-3 ~3 ~ ~-3 minecraft:spruce_planks");
            room(mc, "setblock ~1 ~1 ~-3 femboymod:potted_debug_flytrap");
            room(mc, "setblock ~2 ~1 ~-3 femboymod:potted_catnip");
            room(mc, "setblock ~3 ~1 ~-3 femboymod:potted_grass");
            room(mc, "fill ~-1 ~ ~-6 ~2 ~ ~-5 femboymod:catnip");
        }, SETTLE * 2);
        step("plants_overview", mc -> shot(mc, "plants_overview"), 1);
        step("flytraps", mc -> tpPlayer(mc, origin.getX() - 1.5, origin.getZ() - 1.0, 180.0F, 35.0F), SETTLE);
        step("plants_flytraps", mc -> shot(mc, "plants_flytraps"), 1);
        step("pots", mc -> tpPlayer(mc, origin.getX() + 2.5, origin.getZ() - 1.2, 180.0F, 15.0F), SETTLE);
        step("plants_pots", mc -> shot(mc, "plants_pots"), 1);
        step("catnip", mc -> tpPlayer(mc, origin.getX() + 1.0, origin.getZ() - 3.0, 180.0F, 35.0F), SETTLE);
        step("plants_catnip", mc -> shot(mc, "plants_catnip"), 1);
        step("snack", mc -> {
            tpPlayer(mc, origin.getX() - 2.5, origin.getZ() - 0.8, 180.0F, 35.0F);
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> still(server.getPlayerList().getPlayers().get(0), FemboyEntities.BUG.get(), -4, -3));
        }, PLANTS_SNACK_TICKS);
        step("plants_flytrap_digesting", mc -> shot(mc, "plants_flytrap_digesting"), 1);
        step("check plants", mc -> {
            IntegratedServer server = mc.getSingleplayerServer();
            String result = server.submit(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                boolean bugLeft = !player.level().getEntitiesOfClass(dev.eliasnvx.femboymod.entity.Bug.class,
                        new AABB(origin).inflate(8)).isEmpty();
                StringBuilder digesting = new StringBuilder();
                for (int stage = 0; stage <= DebugFlytrapBlock.MAX_STAGE; stage++) {
                    BlockState state = player.level().getBlockState(origin.offset(stage - 3, 0, -3));
                    if (state.getBlock() instanceof DebugFlytrapBlock && state.getValue(DebugFlytrapBlock.DIGESTING)) {
                        digesting.append(digesting.isEmpty() ? "" : ", ").append("stage ").append(stage);
                    }
                }
                return (bugLeft ? "bug still there" : "bug eaten") + "; digesting: "
                        + (digesting.isEmpty() ? "none" : digesting);
            }).join();
            LOGGER.info("compat harness: flytrap snack: {}", result);
            if (!result.startsWith("bug eaten") || result.endsWith("none")) {
                throw new IllegalStateException("no flytrap ate the bug: " + result);
            }
        }, 0);
    }

    /** Waits up to {@code maxTicks} for {@code done}, then runs {@code then} (also on time out; the checks catch it). */
    private void until(String name, int maxTicks, java.util.function.Predicate<Minecraft> done, Consumer<Minecraft> then) {
        int[] left = {maxTicks};
        Step[] self = new Step[2];
        self[0] = new Step(name, mc -> {
            if (done.test(mc) || left[0]-- <= 0) {
                then.accept(mc);
            } else {
                steps.addFirst(self[1]); // again next tick, without logging the step again
            }
        }, 0);
        self[1] = new Step(null, self[0].action(), 0);
        steps.add(self[0]);
    }

    /** A command run at the room's origin (the player's spot when the scene started). */
    private void room(Minecraft mc, String command) {
        // ".0": whole numbers would be moved to the block's center
        command(mc, String.format(java.util.Locale.ROOT, "execute positioned %d.0 %d %d.0 as @p run %s",
                origin.getX(), origin.getY(), origin.getZ(), command));
    }

    /** A mob without AI (so it holds still for the camera), {@code dx}/{@code dz} blocks from the player, facing them. */
    private <T extends Mob> T still(ServerPlayer player, EntityType<T> type, int dx, int dz) {
        T mob = type.create(player.level());
        if (mob == null) {
            throw new IllegalStateException("could not create " + type);
        }
        mob.moveTo(origin.getX() + 0.5 + dx, origin.getY(), origin.getZ() + 0.5 + dz, 180.0F, 0.0F);
        mob.setYHeadRot(180.0F);
        mob.setYBodyRot(180.0F);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        player.level().addFreshEntity(mob);
        return mob;
    }

    /** Witch Hat, Pumpkin Hoodie and Candy Pail, then the Ghost Sheet. */
    private void planHalloween() {
        step("dress", mc -> wear(mc, FemboyItems.WITCH_HAT.get(), FemboyItems.PUMPKIN_HOODIE.get(), FemboyItems.CANDY_PAIL.get()), SETTLE * 2);
        shots("halloween_pumpkin");
        step("ghost", mc -> wear(mc, FemboyItems.WITCH_HAT.get(), FemboyItems.GHOST_SHEET.get(), FemboyItems.CANDY_PAIL.get()), SETTLE * 2);
        shots("halloween_ghost");
    }

    private static void wear(Minecraft mc, Item head, Item top, Item waist) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(head));
            CosmeticsManager.set(player, FemboySlots.OUTFIT_TOP, new ItemStack(top));
            CosmeticsManager.set(player, FemboySlots.WAIST, new ItemStack(waist));
            CosmeticsManager.set(player, FemboySlots.OUTFIT_BOTTOM, new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
        });
    }

    /** A sitting pet tamed by the player, {@code dx} blocks to the side in the row, facing the player. */
    private TamableAnimal pet(ServerPlayer player, EntityType<? extends TamableAnimal> type, boolean baby, int dx) {
        TamableAnimal pet = type.create(player.level());
        if (pet == null) {
            throw new IllegalStateException("could not create " + type);
        }
        pet.moveTo(origin.getX() + 0.5 + dx, origin.getY(), origin.getZ() + 0.5 + ROW_DISTANCE, 180.0F, 0.0F);
        pet.setYHeadRot(180.0F);
        pet.setYBodyRot(180.0F);
        pet.tame(player);
        pet.setBaby(baby);
        pet.setOrderedToSit(true);
        pet.setInSittingPose(true);
        player.level().addFreshEntity(pet);
        return pet;
    }

    private static void dress(ServerPlayer player, TamableAnimal pet, Item head, Item neck) {
        PetOutfits.putOn(player, pet, PetOutfitItem.Slot.HEAD, new ItemStack(head));
        PetOutfits.putOn(player, pet, PetOutfitItem.Slot.NECK, new ItemStack(neck));
    }

    private static void tpPlayer(Minecraft mc, double x, double z, float yaw) {
        tpPlayer(mc, x, z, yaw, CLOSE_PITCH);
    }

    private static void tpPlayer(Minecraft mc, double x, double z, float yaw, float pitch) {
        command(mc, String.format(java.util.Locale.ROOT, "execute as @p at @s run tp @s %.2f ~ %.2f %.1f %.1f", x, z, yaw, pitch));
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends TamableAnimal>[] petTypes() {
        return new EntityType[]{EntityType.CAT, EntityType.CAT, EntityType.WOLF, EntityType.WOLF, EntityType.PARROT};
    }

    private void tick(Minecraft mc) {
        if (!worldCreated) {
            // the first launch shows the accessibility onboarding instead of the title screen: any screen will do
            if (mc.screen != null && mc.getOverlay() == null && mc.level == null) {
                worldCreated = true;
                createWorld(mc);
            }
            return;
        }
        if (!joined) {
            if (mc.level != null && mc.player != null && mc.screen == null) {
                joined = true;
                wait = JOIN_TICKS;
            }
            return;
        }
        if (mc.screen instanceof PauseScreen) {
            mc.setScreen(null); // paused before the setup step turned pausing off
        }
        Runnable driver = serverDriver;
        if (driver != null) {
            mc.getSingleplayerServer().execute(driver);
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = steps.poll();
        if (step != null) {
            if (step.name() != null) {
                LOGGER.info("compat harness: {}", step.name());
            }
            step.action().accept(mc);
            wait = step.ticksAfter();
        }
    }

    private void step(String name, Consumer<Minecraft> action, int ticksAfter) {
        steps.add(new Step(name, action, ticksAfter));
    }

    /** Front, walking toward the camera, from behind. */
    private void shots(String name) {
        step(name, mc -> shot(mc, name), 1);
        step("walk", mc -> mc.options.keyUp.setDown(true), WALK_TICKS);
        step(name + "_walking", mc -> {
            shot(mc, name + "_walking");
            mc.options.keyUp.setDown(false);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }, SETTLE / 2);
        step(name + "_back", mc -> {
            shot(mc, name + "_back");
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }, SETTLE / 2);
    }

    /** Writes a local Figura avatar (avatar.json + script.lua), loads it on the player and shoots it. */
    private void avatar(String name, String script) {
        step("avatar " + name, mc -> {
            try {
                Path dir = mc.gameDirectory.toPath().resolve("femboymod_compat/avatars/" + name);
                Files.createDirectories(dir);
                Files.writeString(dir.resolve("avatar.json"), "{\"name\": \"femboymod " + name + "\", \"authors\": [\"femboymod test\"]}\n");
                Files.writeString(dir.resolve("script.lua"), script);
                Class.forName("org.figuramc.figura.avatar.AvatarManager").getMethod("loadLocalAvatar", Path.class).invoke(null, dir);
            } catch (IOException | ReflectiveOperationException e) {
                throw new IllegalStateException("could not load the Figura test avatar " + name, e);
            }
        }, AVATAR_TICKS);
        shots("compat_figura_" + name);
    }

    private static final String WORLD = "femboymod compat";

    private static void createWorld(Minecraft mc) {
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, WorldOptions.defaultWithRandomSeed(),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value()
                        .createWorldDimensions());
    }

    private static void dress(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.WOLF_EARS.get()));
            CosmeticsManager.set(player, FemboySlots.OUTFIT_TOP, new ItemStack(FemboyItems.OVERSIZED_HOODIE.get()));
            CosmeticsManager.set(player, FemboySlots.OUTFIT_BOTTOM, new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
            CosmeticsManager.set(player, FemboySlots.HANDS, new ItemStack(FemboyItems.STRIPED_MITTENS.get()));
            CosmeticsManager.set(player, FemboySlots.FEET, new ItemStack(FemboyItems.SNEAKERS.get()));
            CosmeticsManager.set(player, FemboySlots.TAIL, new ItemStack(FemboyItems.TAIL.get()));
            CosmeticsManager.set(player, FemboySlots.BACK, new ItemStack(FemboyItems.CANVAS_BACKPACK.get()));
        });
    }

    private static void command(Minecraft mc, String command) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), message -> LOGGER.info("compat harness: {}", message.getString()));
    }
}
