package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.emote.Emote;
import dev.eliasnvx.femboymod.network.EmotePayloads;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Cosmetics next to mods that change player models and skins (EMF / ETF with Fresh Animations, 3D Skin Layers, Iris
 * shader packs): the player without cosmetics, then in wolf ears and programming socks, standing, walking and from
 * behind, then the same with resource packs on. Skipped unless {@code FEMBOYMOD_COMPAT_SHOTS=1}:
 * {@code FEMBOYMOD_COMPAT_SHOTS=1 ./gradlew :fabric:runClientGameTest -Pcompat_models=true}; files: {@code compat_*.png}.
 * The test run dir is wiped first, so packs come from outside: {@code FEMBOYMOD_COMPAT_PACKS} (a folder of resource pack
 * zips, turned on after the plain shots) and {@code FEMBOYMOD_COMPAT_SHADER} (an Iris shader pack zip, turned on before
 * any shot; needs {@code -Pcompat_render=true}). {@code FEMBOYMOD_COMPAT_TAG} names the run in the file names.
 */
public final class CompatModelsClientTest implements FabricClientGameTest {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("femboymod");
    private static final int SETTLE = 20;
    private static final int WALK_TICKS = 10;
    /** Resource pack reloads take a while with big packs. */
    private static final int RELOAD_TICKS = 200;
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int FOV = 50;
    /** Emotecraft's bundled emotes, by name: kicks (legs, socks, skirt), clapping (arms, mittens), a backflip. */
    private static final List<String> EMOTECRAFT_EMOTES = List.of("kazotsky", "clap", "backflip");
    private static final int EMOTECRAFT_FRAMES = 3;
    private static final int EMOTECRAFT_FRAME_TICKS = 8;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("FEMBOYMOD_COMPAT_SHOTS") == null) {
            return;
        }
        String tag = System.getenv().getOrDefault("FEMBOYMOD_COMPAT_TAG", "models");
        String shader = System.getenv("FEMBOYMOD_COMPAT_SHADER");
        if (shader != null) {
            context.runOnClient(mc -> {
                try {
                    enableShaderPack(Path.of(shader));
                } catch (ReflectiveOperationException | java.io.IOException e) {
                    throw new IllegalStateException("could not turn on the shader pack", e);
                }
            });
            context.waitTicks(RELOAD_TICKS);
        }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 0");
            context.runOnClient(mc -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                mc.options.fov().set(FOV);
                if (!mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle();
                }
            });
            context.waitTicks(SETTLE);
            shots(context, "compat_" + tag + "_bare");
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.WOLF_EARS.get()));
                CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
            });
            context.waitTicks(SETTLE);
            shots(context, "compat_" + tag + "_ears_socks");
            // every resource pack the run dir has (Fresh Animations for EMF), on top of the defaults
            // a whole outfit: third person, first person looking down (First-person Model draws the body there) and
            // one of our emotes (next to Emotecraft's)
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                CosmeticsManager.set(player, FemboySlots.OUTFIT_TOP, new ItemStack(FemboyItems.OVERSIZED_HOODIE.get()));
                CosmeticsManager.set(player, FemboySlots.OUTFIT_BOTTOM, new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
                CosmeticsManager.set(player, FemboySlots.HANDS, new ItemStack(FemboyItems.STRIPED_MITTENS.get()));
                CosmeticsManager.set(player, FemboySlots.FEET, new ItemStack(FemboyItems.SNEAKERS.get()));
                CosmeticsManager.set(player, FemboySlots.TAIL, new ItemStack(FemboyItems.TAIL.get()));
                CosmeticsManager.set(player, FemboySlots.BACK, new ItemStack(FemboyItems.CANVAS_BACKPACK.get()));
            });
            context.waitTicks(SETTLE);
            context.takeScreenshot(TestScreenshotOptions.of("compat_" + tag + "_outfit").withSize(WIDTH, HEIGHT));
            world.getServer().runOnServer(server -> EmotePayloads.play(server.getPlayerList().getPlayers().getFirst(), Emote.HEART_HANDS));
            context.waitTicks(SETTLE / 2);
            context.takeScreenshot(TestScreenshotOptions.of("compat_" + tag + "_outfit_emote").withSize(WIDTH, HEIGHT));
            // Emotecraft's own emotes (legs, arms, the whole body), when it is installed
            context.waitTicks(Emote.HEART_HANDS.durationTicks()); // ours has to end before Emotecraft's starts
            for (String wanted : EMOTECRAFT_EMOTES) {
                String played = context.computeOnClient(mc -> playEmotecraftEmote(wanted));
                if (played == null) {
                    continue;
                }
                for (int i = 0; i < EMOTECRAFT_FRAMES; i++) {
                    context.waitTicks(EMOTECRAFT_FRAME_TICKS);
                    context.takeScreenshot(TestScreenshotOptions.of("compat_" + tag + "_emotecraft_" + wanted + "_" + i).withSize(WIDTH, HEIGHT));
                }
                context.runOnClient(mc -> stopEmotecraftEmote());
                context.waitTicks(SETTLE / 2);
            }
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            world.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 55");
            context.waitTicks(SETTLE);
            context.takeScreenshot(TestScreenshotOptions.of("compat_" + tag + "_first_person").withSize(WIDTH, HEIGHT));
            context.getInput().holdKey(options -> options.keyUp);
            context.waitTicks(WALK_TICKS);
            context.takeScreenshot(TestScreenshotOptions.of("compat_" + tag + "_first_person_walking").withSize(WIDTH, HEIGHT));
            context.getInput().releaseKey(options -> options.keyUp);
            world.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 0");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            context.waitTicks(SETTLE / 2);
            String packDir = System.getenv("FEMBOYMOD_COMPAT_PACKS");
            List<String> packs = packDir == null ? List.of() : context.computeOnClient(mc -> {
                try (var zips = Files.list(Path.of(packDir))) {
                    for (Path zip : zips.filter(path -> path.toString().endsWith(".zip")).toList()) {
                        Files.copy(zip, mc.getResourcePackDirectory().resolve(zip.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (java.io.IOException e) {
                    throw new IllegalStateException("could not copy the resource packs", e);
                }
                mc.getResourcePackRepository().reload();
                List<String> files = new ArrayList<>();
                for (String id : mc.getResourcePackRepository().getAvailableIds()) {
                    if (id.startsWith("file/")) {
                        files.add(id);
                    }
                }
                return files;
            });
            if (!packs.isEmpty()) {
                context.runOnClient(mc -> {
                    List<String> selected = new ArrayList<>(mc.getResourcePackRepository().getSelectedIds());
                    selected.addAll(packs);
                    mc.getResourcePackRepository().setSelected(selected);
                    mc.reloadResourcePacks();
                });
                context.waitTicks(RELOAD_TICKS);
                shots(context, "compat_" + tag + "_packs_ears_socks");
            }
        }
    }

    /**
     * Plays the first Emotecraft emote whose name contains {@code wanted} on the local player; returns its name, or null
     * when Emotecraft isn't installed or has no such emote (Emotecraft's client API by reflection: dev-only test code).
     */
    private static String playEmotecraftEmote(String wanted) {
        try {
            Class<?> api = Class.forName("io.github.kosmx.emotes.api.events.client.ClientEmoteAPI");
            Class<?> animation = Class.forName("com.zigythebird.playeranimcore.animation.Animation");
            for (Object emote : (java.util.Collection<?>) api.getMethod("clientEmoteList").invoke(null)) {
                String name = String.valueOf(animation.getMethod("getNameOrId").invoke(emote));
                if (name.toLowerCase(java.util.Locale.ROOT).contains(wanted)) {
                    boolean started = (boolean) api.getMethod("playEmote", animation).invoke(null, emote);
                    LOGGER.info("Emotecraft emote {}: {}", name, started ? "playing" : "refused");
                    return started ? name : null;
                }
            }
            LOGGER.info("Emotecraft has no emote named like {}", wanted);
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("could not play an Emotecraft emote", e);
        }
    }

    private static void stopEmotecraftEmote() {
        try {
            Class.forName("io.github.kosmx.emotes.api.events.client.ClientEmoteAPI").getMethod("stopEmote").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("could not stop the Emotecraft emote", e);
        }
    }

    /** Copies the pack into Iris's folder and turns it on (Iris internals by reflection: dev-only test code). */
    private static void enableShaderPack(Path zip) throws ReflectiveOperationException, java.io.IOException {
        Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
        Path dir = (Path) iris.getMethod("getShaderpacksDirectory").invoke(null);
        Files.createDirectories(dir);
        Files.copy(zip, dir.resolve(zip.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        Object config = iris.getMethod("getIrisConfig").invoke(null);
        config.getClass().getMethod("setShaderPackName", String.class).invoke(config, zip.getFileName().toString());
        config.getClass().getMethod("setShadersEnabled", boolean.class).invoke(config, true);
        config.getClass().getMethod("save").invoke(config); // reload() reads the config file again
        iris.getMethod("reload").invoke(null);
    }

    private static void shots(ClientGameTestContext context, String name) {
        context.takeScreenshot(TestScreenshotOptions.of(name).withSize(WIDTH, HEIGHT));
        context.getInput().holdKey(options -> options.keyUp);
        context.waitTicks(WALK_TICKS);
        context.takeScreenshot(TestScreenshotOptions.of(name + "_walking").withSize(WIDTH, HEIGHT));
        context.getInput().releaseKey(options -> options.keyUp);
        context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        context.waitTicks(SETTLE / 2);
        context.takeScreenshot(TestScreenshotOptions.of(name + "_back").withSize(WIDTH, HEIGHT));
        context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        context.waitTicks(SETTLE / 2);
    }
}
