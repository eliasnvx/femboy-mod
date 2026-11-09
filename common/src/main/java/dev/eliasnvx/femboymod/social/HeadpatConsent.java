package dev.eliasnvx.femboymod.social;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;

/**
 * Who may give a player headpats: a client option ({@code headpats_from_players}), told to the server on join and
 * whenever it changes. Pets can't opt out; they love it.
 */
public enum HeadpatConsent implements StringRepresentable {
    EVERYONE("everyone"),
    /** Players on the same scoreboard team. */
    TEAMMATES("teammates"),
    NOBODY("nobody");

    public static final Codec<HeadpatConsent> CODEC = StringRepresentable.fromEnum(HeadpatConsent::values);
    private final String name;

    HeadpatConsent(String name) {
        this.name = name;
    }

    /** Whether {@code patted}, with this setting, accepts a headpat from {@code patter}. */
    public boolean allows(Player patted, Player patter) {
        return switch (this) {
            case EVERYONE -> true;
            case TEAMMATES -> patted.getTeam() != null && patted.isAlliedTo(patter);
            case NOBODY -> false;
        };
    }

    /** By network id; anything unknown means the strictest setting. */
    public static HeadpatConsent byId(int id) {
        HeadpatConsent[] values = values();
        return id >= 0 && id < values.length ? values[id] : NOBODY;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
