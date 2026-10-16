package dev.eliasnvx.femboymod.fabric.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;

/**
 * Mirror reflections of a player without cosmetics (only the vanilla model): standing at several distances, with the
 * head turned away from the body, holding a map, walking, at night. Skipped unless {@code FEMBOYMOD_MIRROR_SHOTS=1}:
 * {@code FEMBOYMOD_MIRROR_SHOTS=1 ./gradlew :fabric:runClientGameTest}; files: {@code mirror_bare_*.png}.
 */
public final class MirrorClientTest implements FabricClientGameTest {

    private static final int SETTLE = 10;
    private static final int WALK_TICKS = 8;
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int FOV = 70;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("FEMBOYMOD_MIRROR_SHOTS") == null) {
            return;
        }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("time set midnight");
            world.getServer().runCommand("execute as @p at @s align xz run tp @s ~0.5 ~ ~0.5 0 10");
            world.getServer().runCommand("execute at @p run setblock ~ ~ ~4 femboymod:mirror[facing=north,half=lower]");
            world.getServer().runCommand("execute at @p run setblock ~ ~1 ~4 femboymod:mirror[facing=north,half=upper]");
            context.runOnClient(mc -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.fov().set(FOV);
                if (!mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle();
                }
            });
            context.waitTicks(SETTLE * 2);
            shot(context, "mirror_bare_far");
            world.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~2.4 0 10");
            context.waitTicks(SETTLE);
            shot(context, "mirror_bare_near");
            // head turned, the body stays (it only follows past ~50 degrees)
            context.runOnClient(mc -> mc.player.setYRot(35.0F));
            context.waitTicks(SETTLE);
            shot(context, "mirror_bare_head_turned");
            context.runOnClient(mc -> mc.player.setYRot(0.0F));
            world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:map");
            context.waitTicks(SETTLE);
            shot(context, "mirror_bare_map");
            world.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~-2.4 0 10");
            context.waitTicks(SETTLE);
            shot(context, "mirror_bare_map_far");
            world.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~-2 0 10");
            context.waitTicks(SETTLE);
            context.getInput().holdKey(options -> options.keyUp);
            context.waitTicks(WALK_TICKS);
            shot(context, "mirror_bare_walking");
            context.getInput().releaseKey(options -> options.keyUp);
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            context.waitTicks(SETTLE);
            shot(context, "mirror_bare_behind");
            world.getServer().runCommand("time set noon");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            context.waitTicks(SETTLE);
            shot(context, "mirror_bare_noon");
        }
    }

    private static void shot(ClientGameTestContext context, String name) {
        context.takeScreenshot(TestScreenshotOptions.of(name).withSize(WIDTH, HEIGHT));
    }
}
