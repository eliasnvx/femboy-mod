package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.energy.FemboyEffects;
import dev.eliasnvx.femboymod.entity.FemboyEntities;
import dev.eliasnvx.femboymod.entity.RubberDuck;
import dev.eliasnvx.femboymod.entity.SockGoblin;
import dev.eliasnvx.femboymod.pet.PetOutfitItem;
import dev.eliasnvx.femboymod.pet.PetOutfits;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compat screenshots for 1.20.1, which has no client GameTest API. Only with {@code -Dfemboymod.compat_harness=true}
 * (the {@code runCompatClient} task): creates a flat creative world, dresses the player, shoots them from the front,
 * walking and from behind, leaves and loads the world again and shoots them once more, then quits. With Figura in the runtime ({@code -Pcompat_figura=true}) it then loads two local
 * test avatars and shoots each: one that keeps the vanilla model, one that hides it like most custom avatars do.
 * Files: {@code fabric/build/run/compatClient/screenshots/compat_*.png}. Dev-only test code (Figura by reflection).
 * {@code -Pharness_scene=pets|halloween|mobs|interior|mirror} shoots another scene instead (pet outfits on adults, kittens
 * and puppies, plus a real headpat; Halloween costumes; Sock Goblin and Rubber Duck; the furnished room; the mirror and the vanity table), as
 * on 1.21.1.
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

    private final Deque<Step> steps = new ArrayDeque<>();
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
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = steps.poll();
        if (step != null) {
            LOGGER.info("compat harness: {}", step.name());
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
