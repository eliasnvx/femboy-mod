package dev.eliasnvx.femboymod.fabric.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.TamableAnimal;

/**
 * A furnished room with the 0.2.0 interior pieces, by day and by night, a cat and a dog resting on pet beds, the
 * vanity mirror up close. Skipped unless {@code FEMBOYMOD_INTERIOR_SHOTS=1}:
 * {@code FEMBOYMOD_INTERIOR_SHOTS=1 ./gradlew :fabric:runClientGameTest}; files: {@code interior_*.png}.
 */
public final class InteriorClientTest implements FabricClientGameTest {

    private static final int SETTLE = 10;
    /** Pets look for a bed every 2 s; give them time to walk over and lie down. */
    private static final int PET_TICKS = 400;
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int FOV = 70;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("FEMBOYMOD_INTERIOR_SHOTS") == null) {
            return;
        }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("execute as @p at @s align xz run tp @s ~0.5 ~ ~0.5 180 22");
            // a room in front of the player (north): planks floor, white walls
            run(world, "fill ~-4 ~-1 ~-1 ~4 ~-1 ~-8 minecraft:oak_planks");
            run(world, "fill ~-4 ~ ~-8 ~4 ~3 ~-8 minecraft:white_concrete");
            run(world, "fill ~-4 ~ ~-1 ~-4 ~3 ~-8 minecraft:white_concrete");
            run(world, "fill ~4 ~ ~-1 ~4 ~3 ~-8 minecraft:white_concrete");
            run(world, "setblock ~-2 ~ ~-7 femboymod:vanity_table[facing=south,half=lower]");
            run(world, "setblock ~-2 ~1 ~-7 femboymod:vanity_table[facing=south,half=upper]");
            run(world, "setblock ~-3 ~ ~-5 femboymod:mini_fridge[facing=east,open=true]");
            run(world, "setblock ~-3 ~ ~-3 femboymod:laundry_basket[facing=east,open=true]");
            run(world, "setblock ~2 ~ ~-7 femboymod:bean_bag[facing=south,color=pink]");
            run(world, "setblock ~3 ~ ~-7 femboymod:lava_lamp[color=purple,lit=true]");
            run(world, "setblock ~0 ~ ~-7 femboymod:lava_lamp[color=light_blue,lit=true]");
            for (int x = -1; x <= 3; x++) {
                run(world, "setblock ~" + x + " ~2 ~-7 femboymod:fairy_lights[facing=south,mode=2]");
            }
            run(world, "setblock ~0 ~ ~-4 femboymod:fluffy_rug[shape=heart,color=light_blue]");
            run(world, "setblock ~1 ~ ~-4 femboymod:fluffy_rug[shape=round,color=white]");
            run(world, "setblock ~-1 ~ ~-4 femboymod:fluffy_rug[shape=square,color=pink]");
            run(world, "setblock ~2 ~ ~-3 femboymod:pet_bed[facing=west,color=purple]");
            run(world, "setblock ~2 ~ ~-5 femboymod:pet_bed[facing=west,color=light_blue]");
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                pet(player, EntityTypes.CAT, 1, -2);
                pet(player, EntityTypes.WOLF, -1, -2);
            });
            context.runOnClient(mc -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.fov().set(FOV);
                if (!mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle();
                }
            });
            context.waitTicks(PET_TICKS);
            shot(context, "interior_day");
            world.getServer().runCommand("time set midnight");
            context.waitTicks(SETTLE);
            shot(context, "interior_night");
            // the pet beds from the side: the cat lying, the dog sitting
            world.getServer().runCommand("time set noon");
            run(world, "tp @s ~-1 ~ ~-4 -90 35");
            context.waitTicks(SETTLE);
            shot(context, "interior_pets");
            run(world, "tp @s ~1 ~ ~4 180 22");
            // the vanity mirror up close: the player shows in it
            world.getServer().runCommand("time set noon");
            run(world, "tp @s ~-2 ~ ~-5 180 15");
            context.waitTicks(SETTLE);
            shot(context, "interior_vanity");
            run(world, "tp @s ~2 ~ ~5 180 22");
            // fridge and basket, open
            run(world, "tp @s ~ ~ ~-1 120 30");
            context.waitTicks(SETTLE);
            shot(context, "interior_storage");
        }
    }

    private static void run(TestSingleplayerContext world, String command) {
        world.getServer().runCommand("execute as @p at @s run " + command);
    }

    /** A pet tamed by the player, {@code dx}/{@code dz} blocks from them. */
    private static void pet(ServerPlayer player, net.minecraft.world.entity.EntityType<? extends TamableAnimal> type, int dx, int dz) {
        TamableAnimal pet = type.create(player.level(), EntitySpawnReason.COMMAND);
        if (pet != null) {
            BlockPos pos = player.blockPosition().offset(dx, 0, dz);
            pet.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            pet.tame(player);
            player.level().addFreshEntity(pet);
        }
    }

    private static void shot(ClientGameTestContext context, String name) {
        context.takeScreenshot(TestScreenshotOptions.of(name).withSize(WIDTH, HEIGHT));
    }
}
