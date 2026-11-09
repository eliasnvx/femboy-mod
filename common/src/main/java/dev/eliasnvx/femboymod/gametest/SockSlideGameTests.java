package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.effect.SockSlide;
import dev.eliasnvx.femboymod.entity.FemboyEntities;
import dev.eliasnvx.femboymod.entity.SockGoblin;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Sock sliding (0.3.0): when socks slide, and Sock Goblins sliding on polished floors but not on carpet. 1.20.1 also
 * checks the friction mixin itself (26.3 uses vanilla's friction attribute): a pushed armor stand with slide grip keeps
 * going much further on planks.
 */
public final class SockSlideGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("socks_slide_only_without_shoes", SockSlideGameTests::socksSlideOnlyWithoutShoes),
            new CosmeticGameTests.Entry("goblins_slide_on_polished_floors", SockSlideGameTests::goblinsSlideOnPolishedFloors),
            new CosmeticGameTests.Entry("slide_grip_keeps_momentum", SockSlideGameTests::slideGripKeepsMomentum));

    private static final float EPSILON = 1.0E-5F;
    private static final BlockPos PLANKS_GOBLIN = new BlockPos(1, 2, 1);
    private static final BlockPos CARPET_GOBLIN = new BlockPos(4, 2, 1);
    /** Momentum check: two plank lanes along z, a stand at the start of each, pushed south once it stands. */
    private static final int LANE_LENGTH = 8;
    private static final int GRIPPING_LANE = 1;
    private static final int SLIDING_LANE = 4;
    private static final int SETTLE_TICKS = 2;
    private static final int COAST_TICKS = 20;
    private static final double PUSH = 0.5;
    /** The programming socks' grip; planks then behave like 0.96 friction. */
    private static final float SOCKS_GRIP = 0.1F;
    /** About 1.1 blocks with normal grip, 3.7 with the socks' grip: the slide must at least double it. */
    private static final double SLIDE_VS_GRIP = 2.0;

    private SockSlideGameTests() {
    }

    /** Programming socks and fishnet tights slide (data: cosmetic_stats), but not under shoes or boots. */
    public static void socksSlideOnlyWithoutShoes(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            GameTestAsserts.assertValueEqual(helper, SockSlide.activeGrip(player), 1.0F, "barefoot: no socks, no sliding");
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
            helper.assertTrue(Math.abs(SockSlide.activeGrip(player) - 0.1F) < EPSILON, "socks slide: " + SockSlide.activeGrip(player));
            CosmeticsManager.set(player, FemboySlots.FEET, new ItemStack(FemboyItems.SNEAKERS.get()));
            GameTestAsserts.assertValueEqual(helper, SockSlide.activeGrip(player), 1.0F, "sneakers grip");
            CosmeticsManager.set(player, FemboySlots.FEET, ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            GameTestAsserts.assertValueEqual(helper, SockSlide.activeGrip(player), 1.0F, "boots grip");
            player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.FISHNET_TIGHTS.get()));
            helper.assertTrue(Math.abs(SockSlide.activeGrip(player) - 0.2F) < EPSILON, "tights slide a bit less");
        });
    }

    private static String describe(SockGoblin goblin) {
        return "ticks " + goblin.tickCount + ", no ai " + goblin.isNoAi() + ", grip " + SockSlide.grip(goblin)
                + ", on ground " + goblin.onGround() + ", y " + goblin.getY() + ", below "
                + goblin.level().getBlockState(SockSlide.floorPos(goblin)) + ", feet "
                + goblin.level().getBlockState(goblin.blockPosition()) + ", slippery " + SockSlide.onSlipperyFloor(goblin);
    }

    /**
     * A goblin on planks gets the slide grip and loses it on grass; one on a carpet over planks never gets it
     * (vanilla would read the planks under the carpet).
     */
    public static void goblinsSlideOnPolishedFloors(GameTestHelper helper) {
        helper.setBlock(PLANKS_GOBLIN.below(), Blocks.OAK_PLANKS);
        helper.setBlock(CARPET_GOBLIN.below(), Blocks.OAK_PLANKS);
        helper.setBlock(CARPET_GOBLIN, Blocks.MOSS_CARPET);
        SockGoblin onPlanks = helper.spawnWithNoFreeWill(FemboyEntities.SOCK_GOBLIN.get(), PLANKS_GOBLIN);
        SockGoblin onCarpet = helper.spawnWithNoFreeWill(FemboyEntities.SOCK_GOBLIN.get(), CARPET_GOBLIN);
        float expected = FemboyConfig.common().mobs().sockGoblinSlideGrip();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Math.abs(SockSlide.grip(onPlanks) - expected) < EPSILON,
                    "the goblin on planks slides (" + describe(onPlanks) + ")");
            GameTestAsserts.assertValueEqual(helper, SockSlide.grip(onCarpet), 1.0F, "the carpet grips");
            helper.setBlock(PLANKS_GOBLIN.below(), Blocks.GRASS_BLOCK);
            helper.runAfterDelay(2, () -> {
                GameTestAsserts.assertValueEqual(helper, SockSlide.grip(onPlanks), 1.0F, "on grass: no sliding");
                helper.succeed();
            });
        });
    }

    /**
     * 1.20.1 and 1.21.1 only: the grip really changes movement (the {@code LivingEntity#travel} mixin, on either loader). Two armor
     * stands get the same push on planks; the one with the socks' grip coasts at least twice as far.
     */
    public static void slideGripKeepsMomentum(GameTestHelper helper) {
        for (int z = 0; z < LANE_LENGTH; z++) {
            helper.setBlock(new BlockPos(GRIPPING_LANE, 1, z), Blocks.OAK_PLANKS);
            helper.setBlock(new BlockPos(SLIDING_LANE, 1, z), Blocks.OAK_PLANKS);
        }
        ArmorStand gripping = helper.spawn(EntityType.ARMOR_STAND, new Vec3(GRIPPING_LANE + 0.5, 2, 0.5));
        ArmorStand sliding = helper.spawn(EntityType.ARMOR_STAND, new Vec3(SLIDING_LANE + 0.5, 2, 0.5));
        SockSlide.setGrip(sliding, SOCKS_GRIP);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            double[] start = {gripping.getZ(), sliding.getZ()};
            gripping.setDeltaMovement(0, 0, PUSH);
            sliding.setDeltaMovement(0, 0, PUSH);
            helper.runAfterDelay(COAST_TICKS, () -> {
                double gripped = gripping.getZ() - start[0];
                double slid = sliding.getZ() - start[1];
                helper.assertTrue(slid > gripped * SLIDE_VS_GRIP, "slide grip coasted " + slid + " blocks, normal grip " + gripped);
                helper.succeed();
            });
        });
    }
}
