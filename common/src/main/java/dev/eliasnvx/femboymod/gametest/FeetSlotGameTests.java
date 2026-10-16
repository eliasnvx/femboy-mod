package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.ArmorHiding;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.effect.CosmeticEffectsManager;
import dev.eliasnvx.femboymod.effect.EmulatedAttributes;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The feet slot (0.2.0): shoes and slippers, plus the cat paw gloves. Their effects use 1.21 attributes that 1.20.1
 * lacks; the mod emulates them ({@link EmulatedAttributes}), so the tests check the emulated values.
 */
public final class FeetSlotGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("feet_pieces_wear_and_add_drip", FeetSlotGameTests::feetPiecesWearAndAddDrip),
            new CosmeticGameTests.Entry("shoes_hide_boots", FeetSlotGameTests::shoesHideBoots),
            new CosmeticGameTests.Entry("platform_sneakers_step_up", FeetSlotGameTests::platformSneakersStepUp),
            new CosmeticGameTests.Entry("feet_pieces_emulated_attributes", FeetSlotGameTests::feetPiecesEmulatedAttributes));

    private static final double EPSILON = 1.0E-6;
    /** Data: cosmetic_stats/platform_sneakers.json (+0.4 step height). */
    private static final double PLATFORM_STEP = 0.4;
    /** Vanilla soul sand speed factor and the high-tops' movement efficiency (cosmetic_stats/canvas_high_tops.json). */
    private static final double SOUL_SAND_FACTOR = 0.4;
    private static final double HIGH_TOPS_EFFICIENCY = 0.25;
    /** Data: cosmetic_stats/bunny_slippers.json (+0.2 on the 0.3 sneak factor) and cat_paw_gloves.json (+1.5 blocks). */
    private static final double SLIPPERS_SNEAK = 0.5;
    private static final double PAWS_SAFE_FALL = 1.5;
    /** Soul sand is 14 px tall: a player standing on it has its feet inside the block. */
    private static final double SOUL_SAND_TOP = 14.0 / 16;
    private static final BlockPos SOUL_SAND = new BlockPos(1, 1, 1);

    private FeetSlotGameTests() {
    }

    public static void feetPiecesWearAndAddDrip(GameTestHelper helper) {
        Map<Item, ResourceLocation> slots = Map.of(
                FemboyItems.SNEAKERS.get(), FemboySlots.FEET,
                FemboyItems.CANVAS_HIGH_TOPS.get(), FemboySlots.FEET,
                FemboyItems.PLATFORM_SNEAKERS.get(), FemboySlots.FEET,
                FemboyItems.BUNNY_SLIPPERS.get(), FemboySlots.FEET,
                FemboyItems.CAT_PAW_GLOVES.get(), FemboySlots.HANDS);
        List<ResourceLocation> order = CosmeticsManager.orderedSlots();
        GameTestAsserts.assertValueEqual(helper, order.indexOf(FemboySlots.FEET), order.indexOf(FemboySlots.LEGS_OVERLAY) + 1,
                "feet comes right after socks");
        WearableGameTests.withPlayer(helper, player -> slots.forEach((item, slot) -> {
            ItemStack stack = new ItemStack(item);
            GameTestAsserts.assertValueEqual(helper, CosmeticsManager.slotOf(stack), slot, item + " slot");
            CosmeticsManager.set(player, slot, stack);
            helper.assertTrue(FemboyMod.api().getDripLevel(player).level() > 0, item + " adds drip");
            CosmeticsManager.set(player, slot, ItemStack.EMPTY);
        }));
    }

    public static void shoesHideBoots(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            CosmeticsManager.set(player, FemboySlots.FEET, new ItemStack(FemboyItems.SNEAKERS.get()));
            helper.assertTrue(ArmorHiding.coveredByOutfit(CosmeticsManager.get(player), EquipmentSlot.FEET), "shoes cover boots");
            helper.assertFalse(ArmorHiding.coveredByOutfit(CosmeticsManager.get(player), EquipmentSlot.LEGS), "but not leggings");
            CosmeticsManager.set(player, FemboySlots.FEET, ItemStack.EMPTY);
            helper.assertFalse(ArmorHiding.coveredByOutfit(CosmeticsManager.get(player), EquipmentSlot.FEET), "boots show again");
        });
    }

    /** Platform soles: +0.4 step height while worn (1.20.1: the emulated generic.step_height on maxUpStep). */
    public static void platformSneakersStepUp(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            double base = player.maxUpStep();
            CosmeticsManager.set(player, FemboySlots.FEET, new ItemStack(FemboyItems.PLATFORM_SNEAKERS.get()));
            CosmeticEffectsManager.tick(player);
            helper.assertTrue(Math.abs(player.maxUpStep() - (base + PLATFORM_STEP)) < EPSILON,
                    "step height with platforms: " + player.maxUpStep());
            CosmeticsManager.set(player, FemboySlots.FEET, ItemStack.EMPTY);
            CosmeticEffectsManager.tick(player);
            helper.assertTrue(Math.abs(player.maxUpStep() - base) < EPSILON, "back to normal");
        });
    }

    /**
     * The other 1.21 attributes of the new pieces: high-tops slow you less on soul sand (movement efficiency, checked
     * through a real move), slippers sneak faster (the factor the client applies) and paws make falls safer.
     */
    public static void feetPiecesEmulatedAttributes(GameTestHelper helper) {
        helper.setBlock(SOUL_SAND, Blocks.SOUL_SAND);
        WearableGameTests.withPlayer(helper, player -> {
            Vec3 onSoulSand = Vec3.atBottomCenterOf(helper.absolutePos(SOUL_SAND)).add(0.0, SOUL_SAND_TOP, 0.0);
            player.moveTo(onSoulSand);
            helper.assertTrue(Math.abs(speedFactorAfterMove(player) - SOUL_SAND_FACTOR) < EPSILON, "soul sand slows you");
            CosmeticsManager.set(player, FemboySlots.FEET, new ItemStack(FemboyItems.CANVAS_HIGH_TOPS.get()));
            double expected = SOUL_SAND_FACTOR + (1.0 - SOUL_SAND_FACTOR) * HIGH_TOPS_EFFICIENCY;
            helper.assertTrue(Math.abs(speedFactorAfterMove(player) - expected) < EPSILON, "high-tops: less slowdown on soul sand");

            CosmeticsManager.set(player, FemboySlots.FEET, new ItemStack(FemboyItems.BUNNY_SLIPPERS.get()));
            helper.assertTrue(Math.abs(EmulatedAttributes.value(player, EmulatedAttributes.SNEAKING_SPEED) - SLIPPERS_SNEAK) < EPSILON,
                    "slippers: faster sneaking");
            CosmeticsManager.set(player, FemboySlots.FEET, ItemStack.EMPTY);
            CosmeticsManager.set(player, FemboySlots.HANDS, new ItemStack(FemboyItems.CAT_PAW_GLOVES.get()));
            helper.assertTrue(Math.abs(EmulatedAttributes.bonus(player, EmulatedAttributes.SAFE_FALL_DISTANCE) - PAWS_SAFE_FALL) < EPSILON,
                    "paws: softer falls");
            CosmeticsManager.set(player, FemboySlots.HANDS, ItemStack.EMPTY);
        });
    }

    /** Horizontal speed left after one move of a player sent sideways at speed 1 (the block speed factor). */
    private static double speedFactorAfterMove(ServerPlayer player) {
        player.setDeltaMovement(1.0, 0.0, 0.0);
        player.move(MoverType.SELF, Vec3.ZERO);
        double factor = player.getDeltaMovement().x;
        player.setDeltaMovement(Vec3.ZERO);
        return factor;
    }
}
