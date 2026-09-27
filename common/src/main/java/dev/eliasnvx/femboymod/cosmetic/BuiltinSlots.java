package dev.eliasnvx.femboymod.cosmetic;

import net.minecraft.world.entity.EquipmentSlot;
import java.util.Set;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticSlotType;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Registers the built-in slots of SPEC §4.3 in screen order. */
public final class BuiltinSlots {

    private static final int STEP = 100;

    private BuiltinSlots() {
    }

    /**
     * Built-in slots show a silhouette when empty: sprite {@code femboymod:container/slot/cosmetic/<slot>}.
     * {@code covers}: armor hidden by default while the slot is worn (the armor would draw over the item).
     */
    private static CosmeticSlotType slot(int order, ResourceLocation id, EquipmentSlot... covers) {
        return new CosmeticSlotType(order, Optional.of(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                "container/slot/cosmetic/" + id.getPath())), Set.of(covers));
    }

    public static void register(ApiRegistry<CosmeticSlotType> registry) {
        int order = 0;
        registry.register(FemboySlots.HEAD_ACCESSORY, slot(order += STEP, FemboySlots.HEAD_ACCESSORY, EquipmentSlot.HEAD));
        registry.register(FemboySlots.FACE, slot(order += STEP, FemboySlots.FACE));
        registry.register(FemboySlots.NECK, slot(order += STEP, FemboySlots.NECK));
        registry.register(FemboySlots.OUTFIT_TOP, slot(order += STEP, FemboySlots.OUTFIT_TOP, EquipmentSlot.CHEST));
        registry.register(FemboySlots.HANDS, slot(order += STEP, FemboySlots.HANDS));
        registry.register(FemboySlots.OUTFIT_BOTTOM, slot(order += STEP, FemboySlots.OUTFIT_BOTTOM, EquipmentSlot.LEGS));
        registry.register(FemboySlots.WAIST, slot(order += STEP, FemboySlots.WAIST));
        registry.register(FemboySlots.LEGS_OVERLAY, slot(order += STEP, FemboySlots.LEGS_OVERLAY, EquipmentSlot.LEGS, EquipmentSlot.FEET));
        registry.register(FemboySlots.TAIL, slot(order += STEP, FemboySlots.TAIL));
        registry.register(FemboySlots.BACK, slot(order += STEP, FemboySlots.BACK));
    }
}
