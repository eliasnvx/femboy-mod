package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.block.DebugFlytrapBlock;
import dev.eliasnvx.femboymod.block.FemboyBlocks;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.energy.FemboyEffects;
import dev.eliasnvx.femboymod.entity.Catnip;
import dev.eliasnvx.femboymod.entity.FemboyEntities;
import dev.eliasnvx.femboymod.social.TouchGrass;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 0.3.0 plants in the world: a row of flytraps (stages, open and digesting), the potted plants and catnip; a
 * flytrap snapping up a bug next to it; a cat on catnip; the touch-grass reminder in chat.
 * Skipped unless {@code FEMBOYMOD_PLANT_SHOTS=1}: {@code FEMBOYMOD_PLANT_SHOTS=1 ./gradlew :fabric:runClientGameTest};
 * files: {@code plants_*.png}.
 */
public final class PlantsClientTest implements FabricClientGameTest {

    private static final int SETTLE = 20;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FOV = 70;
    /** Ticks to wait for the flytrap's first check (it polls every half second). */
    private static final int SNAP_WAIT = 40;
    /** The cat sniffs once a second; it should be zooming within two. */
    private static final int ZOOMIES_WAIT = 40;
    private static final int ZOOMIES_SHOTS = 3;
    private static final int ZOOMIES_GAP = 8;
    private static final int PURR_WAIT = 30;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("FEMBOYMOD_PLANT_SHOTS") == null) {
            return;
        }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule spawn_mobs false");
            world.getServer().runCommand("difficulty easy");
            // the player looks north over a garden bed; each scene clears it and sets up the next one
            world.getServer().runCommand("execute as @p at @s align xz run tp @s ~0.5 ~ ~0.5 180 25");
            run(world, "fill ~-8 ~-1 ~-10 ~8 ~-1 ~1 minecraft:grass_block");
            run(world, "fill ~-8 ~ ~-10 ~8 ~4 ~1 minecraft:air");
            BlockPos origin = world.getServer().computeOnServer(server ->
                    server.getPlayerList().getPlayers().getFirst().blockPosition());
            world.getServer().runOnServer(server -> {
                ServerLevel level = server.overworld();
                BlockState trap = FemboyBlocks.DEBUG_FLYTRAP.get().defaultBlockState();
                // left to right: three stages, a digesting one, the potted plants, catnip
                place(level, origin.offset(-4, 0, -5), trap);
                place(level, origin.offset(-3, 0, -5), trap.setValue(DebugFlytrapBlock.STAGE, 1));
                place(level, origin.offset(-2, 0, -5), trap.setValue(DebugFlytrapBlock.STAGE, 2));
                place(level, origin.offset(-1, 0, -5), trap.setValue(DebugFlytrapBlock.STAGE, 2).setValue(DebugFlytrapBlock.DIGESTING, true));
                place(level, origin.offset(0, 0, -5), FemboyBlocks.POTTED_DEBUG_FLYTRAP.get().defaultBlockState());
                place(level, origin.offset(1, 0, -5), FemboyBlocks.POTTED_CATNIP.get().defaultBlockState());
                place(level, origin.offset(2, 0, -5), FemboyBlocks.POTTED_GRASS.get().defaultBlockState());
                place(level, origin.offset(3, 0, -5), FemboyBlocks.CATNIP.get().defaultBlockState());
                place(level, origin.offset(4, 0, -5), FemboyBlocks.CATNIP.get().defaultBlockState());
            });
            context.runOnClient(mc -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.fov().set(FOV);
                if (!mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle();
                }
            });
            context.waitTicks(SETTLE);
            world.getConnection().waitForChunksRender();
            shot(context, "plants_garden");

            // a bug next to a lone flytrap (no AI: it stays put)
            clear(world);
            BlockPos hungry = origin.offset(0, 0, -4);
            UUID bug = world.getServer().computeOnServer(server -> {
                ServerLevel level = server.overworld();
                place(level, hungry, FemboyBlocks.DEBUG_FLYTRAP.get().defaultBlockState().setValue(DebugFlytrapBlock.STAGE, 1));
                var spawned = FemboyEntities.BUG.get().create(level, EntitySpawnReason.COMMAND);
                spawned.snapTo(hungry.getX() + 1.7, hungry.getY(), hungry.getZ() + 0.5, 90.0F, 0.0F);
                spawned.setNoAi(true);
                level.addFreshEntity(spawned);
                return spawned.getUUID();
            });
            context.waitTicks(2);
            shot(context, "plants_bug_near");
            boolean eaten = false;
            for (int tick = 0; tick < SNAP_WAIT && !eaten; tick++) {
                context.waitTick();
                eaten = world.getServer().computeOnServer(server -> server.overworld().getEntity(bug) == null);
            }
            context.waitTicks(3);
            shot(context, "plants_bug_eaten");
            boolean closed = world.getServer().computeOnServer(server ->
                    server.overworld().getBlockState(hungry).getValue(DebugFlytrapBlock.DIGESTING));
            if (!eaten || !closed) {
                throw new AssertionError("the flytrap didn't eat the bug (eaten " + eaten + ", closed " + closed + ")");
            }

            // a cat in a catnip patch: zoomies, then purring
            clear(world);
            UUID catId = world.getServer().computeOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = server.overworld();
                for (int x = -2; x <= 2; x += 2) {
                    place(level, origin.offset(x, 0, -7), FemboyBlocks.CATNIP.get().defaultBlockState());
                }
                Cat cat = EntityTypes.CAT.create(level, EntitySpawnReason.COMMAND);
                cat.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() - 5.5, 180.0F, 0.0F);
                cat.tame(player);
                level.addFreshEntity(cat);
                return cat.getUUID();
            });
            boolean high = false;
            for (int tick = 0; tick < ZOOMIES_WAIT && !high; tick++) {
                context.waitTick();
                high = world.getServer().computeOnServer(server ->
                        server.overworld().getEntity(catId) instanceof Cat cat && Catnip.phase(cat) == Catnip.Phase.ZOOMIES);
            }
            for (int i = 0; i < ZOOMIES_SHOTS; i++) {
                context.waitTicks(ZOOMIES_GAP);
                shot(context, "plants_cat_zoomies_" + i);
            }
            if (!high) {
                throw new AssertionError("the cat didn't smell the catnip");
            }
            world.getServer().runOnServer(server -> {
                if (server.overworld().getEntity(catId) instanceof Cat cat) {
                    var config = FemboyConfig.common().plants();
                    cat.removeEffect(FemboyEffects.CATNIP.asHolder());
                    cat.addEffect(new MobEffectInstance(FemboyEffects.CATNIP.asHolder(),
                            config.catnipToleranceTicks() + config.catnipPurrTicks(), 0, false, false, false));
                    cat.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() - 4.5, 200.0F, 0.0F);
                }
            });
            context.waitTicks(PURR_WAIT);
            shot(context, "plants_cat_purring");

            // the reminder, as the player sees it
            context.runOnClient(mc -> mc.gui.hud.toggle());
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                long now = server.getTickCount();
                long interval = TouchGrass.reminderTicks(FemboyConfig.common().plants());
                TouchGrass.remind(player, new TouchGrass.Session(now - interval, now), now, interval);
            });
            context.waitTicks(5);
            shot(context, "plants_touch_grass_reminder");
        }
    }

    /** Clears the garden bed in front of the player (plants and pots; the grass stays). */
    private static void clear(TestSingleplayerContext world) {
        run(world, "fill ~-8 ~ ~-10 ~8 ~4 ~-1 minecraft:air");
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state);
        if (!level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
            level.setBlockAndUpdate(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        }
    }

    private static void run(TestSingleplayerContext world, String command) {
        world.getServer().runCommand("execute as @p at @s run " + command);
    }

    private static void shot(ClientGameTestContext context, String name) {
        context.takeScreenshot(TestScreenshotOptions.of(name).withSize(WIDTH, HEIGHT));
    }
}
