package dev.eliasnvx.femboymod.api.cosmetic;

import dev.eliasnvx.femboymod.api.FemboyApi;
import net.minecraft.resources.ResourceLocation;

/** Ids of the built-in cosmetic slots (SPEC §4.3). */
public final class FemboySlots {

    /** Ears, hair clips, headphones. */
    public static final ResourceLocation HEAD_ACCESSORY = id("head_accessory");
    /** Glasses, earrings. */
    public static final ResourceLocation FACE = id("face");
    /** Chokers. */
    public static final ResourceLocation NECK = id("neck");
    /** Backpacks. */
    public static final ResourceLocation BACK = id("back");
    /** Tails. */
    public static final ResourceLocation TAIL = id("tail");
    /** Socks and tights. */
    public static final ResourceLocation LEGS_OVERLAY = id("legs_overlay");
    /** Hoodies and sweaters. */
    public static final ResourceLocation OUTFIT_TOP = id("outfit_top");
    /** Skirts. */
    public static final ResourceLocation OUTFIT_BOTTOM = id("outfit_bottom");
    /** Arm warmers, nail polish. */
    public static final ResourceLocation HANDS = id("hands");
    /** Belts and belt chains, worn over skirts and tops. */
    @org.jetbrains.annotations.ApiStatus.AvailableSince("0.1.0")
    public static final ResourceLocation WAIST = id("waist");

    private FemboySlots() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FemboyApi.MOD_ID, path);
    }
}
