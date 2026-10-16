package dev.eliasnvx.femboymod.emote;

/** Built-in emotes (SPEC v1.2): simple arm poses, synced to everyone who sees the player. */
public enum Emote {
    WAVE(60, true),
    PEACE(60, true),
    HEART_HANDS(80, true),
    /** The patting hand of a headpat ({@code Headpats}); not in the emote wheel. */
    HEADPAT(16, false);

    /** How long the emote plays, in ticks; moving cancels it earlier. */
    private final int durationTicks;
    private final boolean inWheel;

    Emote(int durationTicks, boolean inWheel) {
        this.durationTicks = durationTicks;
        this.inWheel = inWheel;
    }

    public int durationTicks() {
        return durationTicks;
    }

    /** Players can pick it in the emote wheel (others are played by the mod itself). */
    public boolean inWheel() {
        return inWheel;
    }

    public String translationKey() {
        return "emote.femboymod." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Emote byId(int id) {
        Emote[] values = values();
        return id >= 0 && id < values.length ? values[id] : WAVE;
    }
}
