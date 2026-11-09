package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.entity.CatKnockGoal;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.level.block.Blocks;

/**
 * A cat knocking a flower pot off a table while the player watches: the stare, then the pot and the flower on the
 * floor. The cat gets a quick, certain knock goal (its own one checks every 10 s with a chance), driven on the server.
 * Skipped unless {@code FEMBOYMOD_CAT_SHOTS=1}: {@code FEMBOYMOD_CAT_SHOTS=1 ./gradlew :fabric:runClientGameTest};
 * files: {@code cats_*.png}.
 */
public final class CatsClientTest implements FabricClientGameTest {

    private static final int SETTLE = 20;
    private static final int MAX_TICKS = 300;
    private static final int AFTER_KNOCK = 25;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FOV = 60;
    private static final CommonConfig.Cats EAGER = new CommonConfig.Cats(20, 1.0F, 6000, 6, 12.0, 30);

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("FEMBOYMOD_CAT_SHOTS") == null) {
            return;
        }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule spawn_mobs false");
            // the player looks north at a two-block table across the room; the pot sits on its east end
            world.getServer().runCommand("execute as @p at @s align xz run tp @s ~0.5 ~ ~0.5 180 20");
            run(world, "fill ~-6 ~-1 ~-8 ~6 ~-1 ~1 minecraft:oak_planks");
            run(world, "fill ~-6 ~ ~-8 ~6 ~4 ~1 minecraft:air");
            run(world, "setblock ~-1 ~ ~-4 minecraft:spruce_planks");
            run(world, "setblock ~ ~ ~-4 minecraft:spruce_planks");
            run(world, "setblock ~ ~1 ~-4 minecraft:potted_poppy");
            BlockPos pot = world.getServer().computeOnServer(server ->
                    server.getPlayerList().getPlayers().getFirst().blockPosition().offset(0, 1, -4));
            UUID catId = world.getServer().computeOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                Cat cat = EntityTypes.CAT.create(player.level(), EntitySpawnReason.COMMAND);
                cat.snapTo(player.getX() - 3, player.getY(), player.getZ() - 4, 90.0F, 0.0F);
                cat.tame(player);
                cat.removeFreeWill(); // only the knock goal below moves it, no strolling or following in between
                player.level().addFreshEntity(cat);
                return cat.getUUID();
            });
            context.runOnClient(mc -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.fov().set(FOV);
                if (!mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle();
                }
            });
            context.waitTicks(SETTLE);
            CatKnockGoal[] goal = new CatKnockGoal[1];
            boolean[] running = {false};
            int closeAt = -1;
            for (int tick = 0; tick < MAX_TICKS; tick++) {
                boolean[] close = {false};
                boolean gone = world.getServer().computeOnServer(server -> {
                    if (!(server.getPlayerList().getPlayers().getFirst().level().getEntity(catId) instanceof Cat cat)) {
                        return true;
                    }
                    if (goal[0] == null) {
                        goal[0] = new CatKnockGoal(cat, () -> EAGER);
                    }
                    if (!running[0]) {
                        running[0] = goal[0].canUse();
                        if (running[0]) {
                            goal[0].start();
                        }
                    } else if (goal[0].canContinueToUse()) {
                        goal[0].tick();
                    } else {
                        goal[0].stop();
                        running[0] = false;
                    }
                    close[0] = cat.position().distanceTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(pot)) < 1.5;
                    return cat.level().getBlockState(pot).isAir();
                });
                if (gone) {
                    break;
                }
                if (close[0] && closeAt < 0) {
                    closeAt = tick;
                }
                context.waitTick();
                // the goal only runs in this loop: shoot in the middle of the stare, between two goal ticks
                if (closeAt >= 0 && tick == closeAt + EAGER.knockStareTicks() * 2 / 3) {
                    shot(context, "cats_stare");
                }
            }
            context.waitTicks(AFTER_KNOCK);
            shot(context, "cats_knocked");
            boolean knocked = world.getServer().computeOnServer(server ->
                    server.getPlayerList().getPlayers().getFirst().level().getBlockState(pot).is(Blocks.AIR));
            if (!knocked) {
                throw new AssertionError("the cat didn't knock the pot off");
            }
        }
    }

    private static void run(TestSingleplayerContext world, String command) {
        world.getServer().runCommand("execute as @p at @s run " + command);
    }

    private static void shot(ClientGameTestContext context, String name) {
        context.takeScreenshot(TestScreenshotOptions.of(name).withSize(WIDTH, HEIGHT));
    }
}
