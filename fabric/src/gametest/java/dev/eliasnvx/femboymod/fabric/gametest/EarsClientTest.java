package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.client.render.CosmeticRenderData;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.function.Function;

/**
 * Reactive ears and tails in game: idle, a mob on the wearer's left and right (checks the ears turn that way), a
 * hissing creeper (flattened ears, puffed tail; bear ears stay up), a hit, low health, a tamed wolf (wag) and the
 * first-person ear tips. Front shots are mirrored: the wearer's left is on the right of the picture.
 * Skipped unless {@code FEMBOYMOD_EAR_SHOTS=1}: {@code FEMBOYMOD_EAR_SHOTS=1 ./gradlew :fabric:runClientGameTest};
 * files: {@code ears_*.png}.
 */
public final class EarsClientTest implements FabricClientGameTest {

    private static final int SETTLE = 20;
    /** A creeper explodes after 30 ticks of swelling; shoot well before. */
    private static final int HISS = 18;
    private static final int HURT_SHOT_DELAY = 2;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 1280;
    private static final int CLOSE_FOV = 30;
    private static final int WIDE_FOV = 70;
    /** Mobs stand this far to the side or in front. */
    private static final int MOB_DISTANCE = 5;
    private static final int CREEPER_DISTANCE = 3;
    private static final int PET_DISTANCE = 2;
    private static final float SOUTH = 0.0F;
    /** The ears must have turned and woken up this much for the direction check. */
    private static final float MIN_ALERT = 0.3F;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("FEMBOYMOD_EAR_SHOTS") == null) {
            return;
        }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule spawn_mobs false");
            world.getServer().runCommand("execute as @p at @s align xz run tp @s ~0.5 ~ ~0.5 " + SOUTH + " 0");
            run(world, "fill ~-9 ~-1 ~-9 ~9 ~-1 ~9 minecraft:grass_block");
            run(world, "fill ~-9 ~ ~-9 ~9 ~5 ~9 minecraft:air");
            wear(world, FemboyItems.CAT_EARS.get());
            context.runOnClient(mc -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                mc.options.fov().set(CLOSE_FOV);
                if (!mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle();
                }
            });
            context.waitTicks(SETTLE);
            shot(context, "ears_idle");

            // facing south, east (+x) is on the wearer's left
            UUID husk = spawn(world, EntityTypes.HUSK, MOB_DISTANCE, 0);
            context.waitTicks(SETTLE);
            expectTurn(context, -1, "a husk on the left");
            shot(context, "ears_notice_left");
            moveTo(world, husk, -MOB_DISTANCE, 0);
            context.waitTicks(SETTLE);
            expectTurn(context, 1, "a husk on the right");
            shot(context, "ears_notice_right");
            // wolf ears are a code model (cat ears are a Blockbench one): both paths react
            wear(world, FemboyItems.WOLF_EARS.get());
            context.waitTicks(SETTLE);
            shot(context, "ears_wolf_notice_right");
            remove(world, husk);
            wear(world, FemboyItems.CAT_EARS.get());

            // behind and to the right, out of the front camera's way
            UUID creeper = spawn(world, EntityTypes.CREEPER, -CREEPER_DISTANCE, -CREEPER_DISTANCE);
            hiss(world, creeper, 1);
            context.waitTicks(HISS);
            shot(context, "ears_creeper_hiss");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            context.waitTicks(1);
            shot(context, "ears_creeper_hiss_tail");
            hiss(world, creeper, -1);
            wear(world, FemboyItems.BEAR_EARS.get());
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            context.waitTicks(SETTLE);
            hiss(world, creeper, 1);
            context.waitTicks(HISS);
            shot(context, "ears_bear_creeper");
            hiss(world, creeper, -1);
            remove(world, creeper);
            wear(world, FemboyItems.CAT_EARS.get());
            context.waitTicks(SETTLE * 2);

            world.getServer().runCommand("damage @p 1 minecraft:generic");
            context.waitTicks(HURT_SHOT_DELAY);
            shot(context, "ears_hurt");
            world.getServer().runCommand("damage @p 15 minecraft:generic");
            context.waitTicks(SETTLE * 2);
            shot(context, "ears_low_health");
            world.getServer().runCommand("effect give @p minecraft:instant_health 1 3");
            context.waitTicks(SETTLE * 2);

            UUID wolf = spawn(world, EntityTypes.WOLF, PET_DISTANCE, -PET_DISTANCE);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                if (player.level().getEntity(wolf) instanceof TamableAnimal pet) {
                    pet.tame(player);
                    pet.setOrderedToSit(true);
                }
            });
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            context.waitTicks(SETTLE);
            shot(context, "ears_pet_wag_1");
            context.waitTicks(3);
            shot(context, "ears_pet_wag_2");
            remove(world, wolf);

            UUID left = spawn(world, EntityTypes.HUSK, MOB_DISTANCE, 1);
            context.runOnClient(mc -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.fov().set(WIDE_FOV);
                mc.gui.hud.toggle(); // the tips are part of the HUD
            });
            context.waitTicks(SETTLE);
            shot(context, "ears_first_person_tips");
            remove(world, left);
        }
    }

    private static void run(TestSingleplayerContext world, String command) {
        world.getServer().runCommand("execute as @p at @s run " + command);
    }

    private static void wear(TestSingleplayerContext world, Item ears) {
        world.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(ears));
            CosmeticsManager.set(player, FemboySlots.TAIL, new ItemStack(FemboyItems.TAIL.get()));
        });
    }

    /** A still mob ({@code dx}, {@code dz}) blocks from the player, looking at nothing in particular. */
    private static UUID spawn(TestSingleplayerContext world, EntityType<? extends Mob> type, int dx, int dz) {
        return world.getServer().computeOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            Mob mob = type.create(player.level(), EntitySpawnReason.COMMAND);
            if (mob == null) {
                throw new IllegalStateException("could not create " + type);
            }
            mob.snapTo(player.getX() + dx, player.getY(), player.getZ() + dz, SOUTH, 0.0F);
            mob.setNoAi(true);
            mob.setPersistenceRequired();
            player.level().addFreshEntity(mob);
            return mob.getUUID();
        });
    }

    private static void moveTo(TestSingleplayerContext world, UUID id, int dx, int dz) {
        world.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            Entity entity = player.level().getEntity(id);
            if (entity != null) {
                entity.snapTo(player.getX() + dx, player.getY(), player.getZ() + dz, SOUTH, 0.0F);
            }
        });
    }

    private static void hiss(TestSingleplayerContext world, UUID id, int direction) {
        onEntity(world, id, entity -> {
            if (entity instanceof Creeper creeper) {
                creeper.setSwellDir(direction);
            }
            return null;
        });
    }

    private static void remove(TestSingleplayerContext world, UUID id) {
        onEntity(world, id, entity -> {
            entity.discard();
            return null;
        });
    }

    private static void onEntity(TestSingleplayerContext world, UUID id, Function<Entity, Void> action) {
        world.getServer().runOnServer(server -> {
            Entity entity = server.getPlayerList().getPlayers().getFirst().level().getEntity(id);
            if (entity != null) {
                action.apply(entity);
            }
        });
    }

    /** The local player's ears are alert and turned to {@code side} (-1 left, 1 right). */
    private static void expectTurn(ClientGameTestContext context, int side, String what) {
        String problem = context.computeOnClient(mc -> {
            CosmeticRenderData data = CosmeticRenderData.of(mc.player);
            if (data == null) {
                return "no render data for the player";
            }
            if (data.earAlert() < MIN_ALERT) {
                return "ears not alert for " + what + ": " + data.earAlert();
            }
            return Math.signum(data.earYaw()) == side ? null : "ears turned the wrong way for " + what + ": " + data.earYaw();
        });
        if (problem != null) {
            throw new AssertionError(problem);
        }
    }

    private static void shot(ClientGameTestContext context, String name) {
        context.takeScreenshot(TestScreenshotOptions.of(name).withSize(WIDTH, HEIGHT));
    }
}
