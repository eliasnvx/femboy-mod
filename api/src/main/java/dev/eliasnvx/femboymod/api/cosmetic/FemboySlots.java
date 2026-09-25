package dev.eliasnvx.femboymod.api.cosmetic;

import dev.eliasnvx.femboymod.api.FemboyApi;
import net.minecraft.resources.Identifier;

/** Ids of the built-in cosmetic slots (SPEC §4.3). */
public final class FemboySlots {

    /** Ears, hair clips, headphones. */
    public static final Identifier HEAD_ACCESSORY = id("head_accessory");
    /** Glasses, earrings. */
    public static final Identifier FACE = id("face");
    /** Chokers. */
    public static final Identifier NECK = id("neck");
    /** Backpacks. */
    public static final Identifier BACK = id("back");
    /** Tails. */
    public static final Identifier TAIL = id("tail");
    /** Socks and tights. */
    public static final Identifier LEGS_OVERLAY = id("legs_overlay");
    /** Hoodies and sweaters. */
    public static final Identifier OUTFIT_TOP = id("outfit_top");
    /** Skirts. */
    public static final Identifier OUTFIT_BOTTOM = id("outfit_bottom");
    /** Arm warmers, nail polish. */
    public static final Identifier HANDS = id("hands");

    private FemboySlots() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FemboyApi.MOD_ID, path);
    }
}
