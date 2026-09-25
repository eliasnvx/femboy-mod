package dev.eliasnvx.femboymod.cosmetic;

import dev.eliasnvx.femboymod.api.cosmetic.CosmeticSlotType;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;

/** Registers the built-in slots of SPEC §4.3 in screen order. */
public final class BuiltinSlots {

    private static final int STEP = 100;

    private BuiltinSlots() {
    }

    public static void register(ApiRegistry<CosmeticSlotType> registry) {
        int order = 0;
        registry.register(FemboySlots.HEAD_ACCESSORY, new CosmeticSlotType(order += STEP));
        registry.register(FemboySlots.FACE, new CosmeticSlotType(order += STEP));
        registry.register(FemboySlots.NECK, new CosmeticSlotType(order += STEP));
        registry.register(FemboySlots.OUTFIT_TOP, new CosmeticSlotType(order += STEP));
        registry.register(FemboySlots.HANDS, new CosmeticSlotType(order += STEP));
        registry.register(FemboySlots.OUTFIT_BOTTOM, new CosmeticSlotType(order += STEP));
        registry.register(FemboySlots.LEGS_OVERLAY, new CosmeticSlotType(order += STEP));
        registry.register(FemboySlots.TAIL, new CosmeticSlotType(order += STEP));
        registry.register(FemboySlots.BACK, new CosmeticSlotType(order += STEP));
    }
}
