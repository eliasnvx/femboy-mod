package dev.eliasnvx.femboymod.emote;

/** Built-in emotes (SPEC v1.2): simple arm poses, synced to everyone who sees the player. */
public enum Emote {
    WAVE(60),
    PEACE(60),
    HEART_HANDS(80);

    /** How long the emote plays, in ticks; moving cancels it earlier. */
    private final int durationTicks;

    Emote(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public String translationKey() {
        return "emote.femboymod." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Emote byId(int id) {
        Emote[] values = values();
        return id >= 0 && id < values.length ? values[id] : WAVE;
    }
}
