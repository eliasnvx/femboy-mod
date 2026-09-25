package dev.eliasnvx.femboymod.api.drip;

/**
 * A computed Drip Level.
 *
 * @param level 0..{@link DripRules#maxLevel()}
 * @param tier  0..number of tier thresholds
 */
public record DripLevel(int level, int tier) {

    /** Nothing worn. */
    public static final DripLevel NONE = new DripLevel(0, 0);
}
