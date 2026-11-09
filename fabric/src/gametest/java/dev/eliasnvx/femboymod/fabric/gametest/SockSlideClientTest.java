package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.CameraType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Sock sliding with a real client (players move on their own client): run along a plank floor, let go, and measure
 * how far the player keeps going in socks, in sneakers and in socks on a carpet. Socks must coast clearly further,
 * and the coast must earn Parquet Drift on the server.
 * Skipped unless {@code FEMBOYMOD_SLIDE_SHOTS=1}: {@code FEMBOYMOD_SLIDE_SHOTS=1 ./gradlew :fabric:runClientGameTest};
 * files: {@code slide_*.png}.
 */
public final class SockSlideClientTest implements FabricClientGameTest {

    private static final int SETTLE = 20;
    private static final int RUN_TICKS = 25;
    private static final int COAST_TICKS = 30;
    /** Socks must coast at least this many times further than sneakers. */
    private static final double SOCKS_VS_SHOES = 1.5;
    private static final int FLOOR = 24;
    private static final float SOUTH = 0.0F;
    private static final Identifier DRIFT = Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "main/sock_drift");

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("FEMBOYMOD_SLIDE_SHOTS") == null) {
            return;
        }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule spawn_mobs false");
            world.getServer().runCommand("gamemode survival @p");
            world.getServer().runCommand("execute as @p at @s align xz run tp @s ~0.5 ~ ~0.5 " + SOUTH + " 0");
            run(world, "fill ~-3 ~-1 ~-2 ~3 ~-1 ~" + FLOOR + " minecraft:oak_planks");
            run(world, "fill ~-3 ~ ~-2 ~3 ~3 ~" + FLOOR + " minecraft:air");
            Vec3 start = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().position());
            context.runOnClient(mc -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                if (!mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle();
                }
            });

            wear(world, FemboyItems.PROGRAMMING_SOCKS.get(), null);
            double socks = coast(context, world, start, "slide_socks");
            wear(world, FemboyItems.PROGRAMMING_SOCKS.get(), FemboyItems.SNEAKERS.get());
            double shoes = coast(context, world, start, "slide_sneakers");
            wear(world, FemboyItems.PROGRAMMING_SOCKS.get(), null);
            run(world, "fill ~-3 ~ ~-2 ~3 ~ ~" + FLOOR + " minecraft:moss_carpet");
            double carpet = coast(context, world, start.add(0, 1.0 / 16, 0), "slide_carpet");
            FemboyMod.LOGGER.info("Sock slide coast: socks {} blocks, sneakers {}, socks on carpet {}", socks, shoes, carpet);
            if (socks < shoes * SOCKS_VS_SHOES) {
                throw new AssertionError("socks coasted " + socks + " blocks, sneakers " + shoes);
            }
            if (carpet > shoes * SOCKS_VS_SHOES) {
                throw new AssertionError("the carpet didn't grip: " + carpet + " blocks, sneakers " + shoes);
            }
            boolean drifted = world.getServer().computeOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                AdvancementHolder drift = server.getAdvancements().get(DRIFT);
                return drift != null && player.getAdvancements().getOrStartProgress(drift).isDone();
            });
            if (!drifted) {
                throw new AssertionError("coasting in socks didn't earn " + DRIFT);
            }
        }
    }

    private static void run(TestSingleplayerContext world, String command) {
        world.getServer().runCommand("execute as @p at @s run " + command);
    }

    private static void wear(TestSingleplayerContext world, Item legs, Item feet) {
        world.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(legs));
            CosmeticsManager.set(player, FemboySlots.FEET, feet == null ? ItemStack.EMPTY : new ItemStack(feet));
        });
    }

    /** Runs forward from {@code start}, lets go and returns how far the player kept going (blocks). */
    private static double coast(ClientGameTestContext context, TestSingleplayerContext world, Vec3 start, String shot) {
        world.getServer().runCommand(String.format(java.util.Locale.ROOT, "tp @p %.3f %.4f %.3f %.1f 0",
                start.x, start.y, start.z, SOUTH));
        context.waitTicks(SETTLE);
        context.getInput().holdKeyFor(options -> options.keyUp, RUN_TICKS);
        Vec3 released = context.computeOnClient(mc -> mc.player.position());
        context.waitTicks(2);
        context.takeScreenshot(TestScreenshotOptions.of(shot));
        context.waitTicks(COAST_TICKS - 2);
        Vec3 stopped = context.computeOnClient(mc -> mc.player.position());
        return stopped.subtract(released).horizontalDistance();
    }
}
