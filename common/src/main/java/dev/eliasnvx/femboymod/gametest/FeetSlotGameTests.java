package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.ArmorHiding;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.effect.CosmeticEffectsManager;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The feet slot (0.2.0): shoes and slippers, plus the cat paw gloves. */
public final class FeetSlotGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("feet_pieces_wear_and_add_drip", FeetSlotGameTests::feetPiecesWearAndAddDrip),
            new CosmeticGameTests.Entry("shoes_hide_boots", FeetSlotGameTests::shoesHideBoots),
            new CosmeticGameTests.Entry("platform_sneakers_step_up", FeetSlotGameTests::platformSneakersStepUp));

    private static final double EPSILON = 1.0E-6;

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
        helper.assertValueEqual(order.indexOf(FemboySlots.FEET), order.indexOf(FemboySlots.LEGS_OVERLAY) + 1,
                "feet comes right after socks");
        WearableGameTests.withPlayer(helper, player -> slots.forEach((item, slot) -> {
            ItemStack stack = new ItemStack(item);
            helper.assertValueEqual(CosmeticsManager.slotOf(stack), slot, item + " slot");
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

    /** Platform soles: +0.4 step height while worn (data: cosmetic_stats/platform_sneakers.json). */
    public static void platformSneakersStepUp(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            double base = player.getAttributeValue(Attributes.STEP_HEIGHT);
            CosmeticsManager.set(player, FemboySlots.FEET, new ItemStack(FemboyItems.PLATFORM_SNEAKERS.get()));
            CosmeticEffectsManager.tick(player);
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.STEP_HEIGHT) - (base + 0.4)) < EPSILON,
                    "step height with platforms: " + player.getAttributeValue(Attributes.STEP_HEIGHT));
            CosmeticsManager.set(player, FemboySlots.FEET, ItemStack.EMPTY);
            CosmeticEffectsManager.tick(player);
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.STEP_HEIGHT) - base) < EPSILON, "back to normal");
        });
    }
}
