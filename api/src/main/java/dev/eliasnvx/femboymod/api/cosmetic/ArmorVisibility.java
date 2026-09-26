package dev.eliasnvx.femboymod.api.cosmetic;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.ApiStatus;

/**
 * How one vanilla armor piece is drawn on a player who wears cosmetics. Only the look changes: the armor
 * still protects. Servers can forbid hiding armor with the {@code femboymod:allow_hidden_armor} game rule.
 */
@ApiStatus.AvailableSince("0.1.0")
public enum ArmorVisibility implements StringRepresentable {
    /** Hidden while a worn cosmetic covers that body part ({@link CosmeticSlotType#coversArmor()}); the default. */
    AUTO("auto"),
    /** Always drawn. */
    SHOW("show"),
    /** Never drawn. */
    HIDE("hide");

    /** Codec by lowercase name. */
    public static final Codec<ArmorVisibility> CODEC = StringRepresentable.fromEnum(ArmorVisibility::values);

    private final String name;

    ArmorVisibility(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /**
     * @return the next state when cycling through them in a UI: auto, hide, show
     */
    public ArmorVisibility next() {
        return switch (this) {
            case AUTO -> HIDE;
            case HIDE -> SHOW;
            case SHOW -> AUTO;
        };
    }
}
