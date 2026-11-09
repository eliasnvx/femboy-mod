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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Sock sliding (0.3.0): when socks slide, and Sock Goblins sliding on polished floors but not on carpet. */
public final class SockSlideGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("socks_slide_only_without_shoes", SockSlideGameTests::socksSlideOnlyWithoutShoes),
            new CosmeticGameTests.Entry("goblins_slide_on_polished_floors", SockSlideGameTests::goblinsSlideOnPolishedFloors));

    private static final float EPSILON = 1.0E-5F;
    private static final BlockPos PLANKS_GOBLIN = new BlockPos(1, 2, 1);
    private static final BlockPos CARPET_GOBLIN = new BlockPos(4, 2, 1);

    private SockSlideGameTests() {
    }

    /** Programming socks and fishnet tights slide (data: cosmetic_stats), but not under shoes or boots. */
    public static void socksSlideOnlyWithoutShoes(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            helper.assertValueEqual(SockSlide.activeGrip(player), 1.0F, "barefoot: no socks, no sliding");
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
            helper.assertTrue(Math.abs(SockSlide.activeGrip(player) - 0.1F) < EPSILON, "socks slide: " + SockSlide.activeGrip(player));
            CosmeticsManager.set(player, FemboySlots.FEET, new ItemStack(FemboyItems.SNEAKERS.get()));
            helper.assertValueEqual(SockSlide.activeGrip(player), 1.0F, "sneakers grip");
            CosmeticsManager.set(player, FemboySlots.FEET, ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            helper.assertValueEqual(SockSlide.activeGrip(player), 1.0F, "boots grip");
            player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.FISHNET_TIGHTS.get()));
            helper.assertTrue(Math.abs(SockSlide.activeGrip(player) - 0.2F) < EPSILON, "tights slide a bit less");
        });
    }

    private static String describe(SockGoblin goblin) {
        return "ticks " + goblin.tickCount + ", no ai " + goblin.isNoAi() + ", friction " + goblin.getAttributeValue(Attributes.FRICTION_MODIFIER)
                + ", on ground " + goblin.onGround() + ", y " + goblin.getY() + ", below "
                + goblin.level().getBlockState(goblin.getBlockPosBelowThatAffectsMyMovement()) + ", feet "
                + goblin.level().getBlockState(goblin.blockPosition()) + ", slippery " + SockSlide.onSlipperyFloor(goblin);
    }

    /**
     * A goblin on planks gets the friction modifier and loses it on grass; one on a carpet over planks never gets it
     * (vanilla would read the planks under the carpet).
     */
    public static void goblinsSlideOnPolishedFloors(GameTestHelper helper) {
        helper.setBlock(PLANKS_GOBLIN.below(), Blocks.OAK_PLANKS);
        helper.setBlock(CARPET_GOBLIN.below(), Blocks.OAK_PLANKS);
        helper.setBlock(CARPET_GOBLIN, Blocks.MOSS_CARPET);
        SockGoblin onPlanks = helper.spawnWithNoFreeWill(FemboyEntities.SOCK_GOBLIN.get(), PLANKS_GOBLIN);
        SockGoblin onCarpet = helper.spawnWithNoFreeWill(FemboyEntities.SOCK_GOBLIN.get(), CARPET_GOBLIN);
        double expected = FemboyConfig.common().mobs().sockGoblinSlideGrip() - 1.0;
        helper.runAfterDelay(2, () -> {
            AttributeModifier slide = onPlanks.getAttribute(Attributes.FRICTION_MODIFIER).getModifier(SockSlide.MODIFIER_ID);
            helper.assertTrue(slide != null && Math.abs(slide.amount() - expected) < EPSILON, "the goblin on planks slides ("
                    + describe(onPlanks) + ", modifier " + (slide == null ? "none" : slide.amount()) + ")");
            helper.assertFalse(onCarpet.getAttribute(Attributes.FRICTION_MODIFIER).hasModifier(SockSlide.MODIFIER_ID),
                    "the carpet grips");
            helper.setBlock(PLANKS_GOBLIN.below(), Blocks.GRASS_BLOCK);
            helper.runAfterDelay(2, () -> {
                helper.assertFalse(onPlanks.getAttribute(Attributes.FRICTION_MODIFIER).hasModifier(SockSlide.MODIFIER_ID),
                        "on grass: no sliding");
                helper.succeed();
            });
        });
    }
}
