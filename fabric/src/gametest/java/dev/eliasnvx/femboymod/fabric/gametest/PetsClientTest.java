package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.energy.FemboyEffects;
import dev.eliasnvx.femboymod.pet.PetOutfitItem;
import dev.eliasnvx.femboymod.pet.PetOutfits;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Pet outfits on adults and on the kitten and puppy models (the whole row, then each baby up close from the front and
 * the side, then with the other pieces), then a real headpat: sneak + right-click on the kitten with an empty hand,
 * checked on the server (Cozy, still sitting) and shot in first and third person.
 * Skipped unless {@code FEMBOYMOD_PET_SHOTS=1}: {@code FEMBOYMOD_PET_SHOTS=1 ./gradlew :fabric:runClientGameTest};
 * files: {@code pets_*.png}.
 */
public final class PetsClientTest implements FabricClientGameTest {

    private static final int SETTLE = 20;
    /** The pat emote and the hearts are short: shoot while they play. */
    private static final int PAT_TICKS = 4;
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int FOV = 60;
    /** How far in front of the player the row of pets sits, and the gap between them. */
    private static final int ROW_DISTANCE = 3;
    private static final int GAP = 2;
    private static final float FACING_PLAYER = 0.0F;
    /** The row is shot from further back than it stands. */
    private static final int ROW_SHOT_BACK = 3;
    /** Close-ups: this far from the pet, looking down this much. */
    private static final double CLOSE = 1.6;
    private static final float CLOSE_PITCH = 30.0F;
    private static final float NORTH = 180.0F;
    private static final float WEST = 90.0F;
    /** Standing this far behind the kitten and looking this far down puts the crosshair on it while sneaking. */
    private static final double PAT_DISTANCE = 1.2;
    private static final float PAT_PITCH = 45.0F;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("FEMBOYMOD_PET_SHOTS") == null) {
            return;
        }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("execute as @p at @s align xz run tp @s ~0.5 ~ ~0.5 180 25");
            run(world, "fill ~-5 ~-1 ~1 ~5 ~-1 ~-6 minecraft:moss_block");
            run(world, "fill ~-5 ~ ~1 ~5 ~4 ~-6 minecraft:air");
            List<UUID> pets = world.getServer().computeOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                List<UUID> ids = new ArrayList<>();
                ids.add(pet(player, EntityTypes.CAT, false, -3, FemboyItems.PET_BOW.get(), FemboyItems.BELL_COLLAR.get()));
                ids.add(pet(player, EntityTypes.CAT, true, -1, FemboyItems.MINI_WITCH_HAT.get(), FemboyItems.PET_BANDANA.get()));
                ids.add(pet(player, EntityTypes.WOLF, false, 1, FemboyItems.MINI_WITCH_HAT.get(), FemboyItems.BELL_COLLAR.get()));
                ids.add(pet(player, EntityTypes.WOLF, true, 3, FemboyItems.PET_BOW.get(), FemboyItems.PET_BANDANA.get()));
                return ids;
            });
            context.runOnClient(mc -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.fov().set(FOV);
                if (!mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle();
                }
            });
            run(world, "tp @s ~ ~ ~" + ROW_SHOT_BACK + " " + NORTH + " 20");
            context.waitTicks(SETTLE);
            shot(context, "pets_row");
            closeUps(context, world, pets, "pets");
            // the other pieces on each
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                Item[][] swapped = {
                        {FemboyItems.MINI_WITCH_HAT.get(), FemboyItems.PET_BANDANA.get()},
                        {FemboyItems.PET_BOW.get(), FemboyItems.BELL_COLLAR.get()},
                        {FemboyItems.PET_BOW.get(), FemboyItems.PET_BANDANA.get()},
                        {FemboyItems.MINI_WITCH_HAT.get(), FemboyItems.BELL_COLLAR.get()}};
                for (int i = 0; i < pets.size(); i++) {
                    if (player.level().getEntity(pets.get(i)) instanceof TamableAnimal pet) {
                        dress(player, pet, swapped[i][0], swapped[i][1]);
                    }
                }
            });
            closeUps(context, world, pets, "pets_swapped");
            // a real headpat on the kitten: stand behind it, look down at it, sneak and right-click (the swap put the old
            // pieces in the player's hands)
            world.getServer().runCommand("clear @p");
            BlockPos kitten = world.getServer().computeOnServer(server ->
                    server.getPlayerList().getPlayers().getFirst().level().getEntity(pets.get(1)).blockPosition());
            world.getServer().runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %d %.2f %.1f %.1f",
                    kitten.getX() + 0.5, kitten.getY(), kitten.getZ() + 0.5 + PAT_DISTANCE, NORTH, PAT_PITCH));
            context.getInput().holdKey(options -> options.keyShift);
            context.waitTicks(SETTLE / 2);
            context.getInput().pressKey(options -> options.keyUse);
            context.waitTicks(PAT_TICKS);
            shot(context, "pets_headpat_first_person");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            context.waitTicks(1);
            shot(context, "pets_headpat_third_person");
            context.getInput().releaseKey(options -> options.keyShift);
            String problem = world.getServer().computeOnServer(server -> {
                if (!(server.getPlayerList().getPlayers().getFirst().level().getEntity(pets.get(1)) instanceof TamableAnimal pet)) {
                    return "the kitten is gone";
                }
                if (!pet.hasEffect(FemboyEffects.COZY.asHolder())) {
                    return "the kitten is not Cozy after the headpat";
                }
                return pet.isOrderedToSit() ? null : "the headpat also made the kitten stand up";
            });
            if (problem != null) {
                throw new AssertionError(problem);
            }
        }
    }

    private static void run(TestSingleplayerContext world, String command) {
        world.getServer().runCommand("execute as @p at @s run " + command);
    }

    /** A sitting pet tamed by the player, {@code dx} blocks to the side in the row, facing the player, dressed. */
    private static UUID pet(ServerPlayer player, EntityType<? extends TamableAnimal> type, boolean baby, int dx, Item head, Item neck) {
        TamableAnimal pet = type.create(player.level(), EntitySpawnReason.COMMAND);
        if (pet == null) {
            throw new IllegalStateException("could not create " + type);
        }
        BlockPos pos = player.blockPosition().offset(dx * GAP / 2, 0, -ROW_DISTANCE);
        pet.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, FACING_PLAYER, 0.0F);
        pet.setYHeadRot(FACING_PLAYER);
        pet.setYBodyRot(FACING_PLAYER);
        pet.tame(player);
        pet.setBaby(baby);
        pet.setOrderedToSit(true);
        pet.setInSittingPose(true);
        player.level().addFreshEntity(pet);
        dress(player, pet, head, neck);
        return pet.getUUID();
    }

    private static void dress(ServerPlayer player, TamableAnimal pet, Item head, Item neck) {
        PetOutfits.putOn(player, pet, PetOutfitItem.Slot.HEAD, new ItemStack(head));
        PetOutfits.putOn(player, pet, PetOutfitItem.Slot.NECK, new ItemStack(neck));
    }

    /** The kitten (second in the row) and the puppy (fourth), each from the front and from its right side. */
    private static void closeUps(ClientGameTestContext context, TestSingleplayerContext world, List<UUID> pets, String name) {
        int[] babies = {1, 3};
        String[] names = {"kitten", "puppy"};
        for (int i = 0; i < babies.length; i++) {
            UUID id = pets.get(babies[i]);
            BlockPos pos = world.getServer().computeOnServer(server ->
                    server.getPlayerList().getPlayers().getFirst().level().getEntity(id).blockPosition());
            double x = pos.getX() + 0.5;
            double z = pos.getZ() + 0.5;
            tp(world, x, pos.getY(), z + CLOSE, NORTH);
            context.waitTicks(SETTLE);
            shot(context, name + "_" + names[i] + "_front");
            tp(world, x + CLOSE, pos.getY(), z, WEST);
            context.waitTicks(SETTLE);
            shot(context, name + "_" + names[i] + "_side");
        }
    }

    private static void tp(TestSingleplayerContext world, double x, int y, double z, float yaw) {
        world.getServer().runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %d %.2f %.1f %.1f", x, y, z, yaw, CLOSE_PITCH));
    }

    private static void shot(ClientGameTestContext context, String name) {
        context.takeScreenshot(TestScreenshotOptions.of(name).withSize(WIDTH, HEIGHT));
    }
}
