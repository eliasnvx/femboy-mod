package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.compat.ExternalSlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** Cosmetics in other mods' slots (Curios, Trinkets, Accessories) count as worn; tested with a fake slot mod. */
public final class ExternalSlotsGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("external_slots_count_as_worn", ExternalSlotsGameTests::externalSlotsCountAsWorn));

    private ExternalSlotsGameTests() {
    }

    public static void externalSlotsCountAsWorn(GameTestHelper helper) {
        ItemStack ears = new ItemStack(FemboyItems.CAT_EARS.get());
        ItemStack backpack = new ItemStack(FemboyItems.CANVAS_BACKPACK.get());
        ExternalSlots.Source fake = (player, action) -> {
            action.accept(ItemStack.EMPTY);
            action.accept(ears);
            action.accept(backpack);
        };
        ExternalSlots.register(fake);
        try {
            WearableGameTests.withPlayer(helper, player -> {
                helper.assertTrue(CosmeticsManager.get(player).get(FemboySlots.HEAD_ACCESSORY).isEmpty(), "our slot stays empty");
                helper.assertTrue(CosmeticsManager.worn(player).get(FemboySlots.HEAD_ACCESSORY).is(FemboyItems.CAT_EARS.get()),
                        "ears from the other mod's slot are worn");
                helper.assertTrue(CosmeticsManager.worn(player) == CosmeticsManager.worn(player), "same instance while nothing changes");
                helper.assertTrue(FemboyMod.api().getDripLevel(player).level() > 0, "and add Drip");
                helper.assertTrue(CosmeticsManager.worn(player).get(FemboySlots.BACK).isEmpty(), "backpacks only count in our slot");
                CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.FOX_EARS.get()));
                helper.assertTrue(CosmeticsManager.worn(player).get(FemboySlots.HEAD_ACCESSORY).is(FemboyItems.FOX_EARS.get()),
                        "our own slot wins");
                CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, ItemStack.EMPTY);
            });
        } finally {
            ExternalSlots.unregister(fake);
        }
    }
}
