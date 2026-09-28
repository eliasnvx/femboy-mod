package dev.eliasnvx.femboymod.world;

import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameRules;

/**
 * Per-world switches for server admins, changed in game with {@code /gamerule femboymod:<name> <value>}.
 * They complement the global config file: a feature runs only if both allow it.
 * <p>
 * 1.20.1 has no game rule registry: the rules go through {@link GameRules#register} under the same names as on 26.3
 * ({@code femboymod:style_points}, ...). Read them with {@code level.getGameRules().getBoolean(RULE)} and
 * {@link #dripPvpPercent(GameRules)}. The lang key is {@code gamerule.femboymod:<name>} ({@link GameRules.Key#getDescriptionId()}).
 */
public final class FemboyGameRules {

    /** Players earn Style Points. */
    public static final GameRules.Key<GameRules.BooleanValue> STYLE_POINTS = bool("style_points", GameRules.Category.PLAYER, true);
    /** Set bonuses apply (also needs {@code set_bonuses_enabled} in the config). */
    public static final GameRules.Key<GameRules.BooleanValue> SET_BONUSES = bool("set_bonuses", GameRules.Category.PLAYER, true);
    /** Cosmetics stay on when the player dies (like keepInventory, only for cosmetic slots). */
    public static final GameRules.Key<GameRules.BooleanValue> KEEP_COSMETICS = bool("keep_cosmetics", GameRules.Category.DROPS, false);
    /** Players may hide their armor under the outfit; off = armor is always drawn (PvP servers). */
    public static final GameRules.Key<GameRules.BooleanValue> ALLOW_HIDDEN_ARMOR = bool("allow_hidden_armor", GameRules.Category.PLAYER, true);

    /** Range of {@link #DRIP_PVP_PERCENT}; 1.20.1 integer rules have no built-in bounds, so values are clamped. */
    public static final int DRIP_PVP_MIN = 0;
    public static final int DRIP_PVP_MAX = 50;
    /**
     * PvP: percent more damage per Drip tier the attacker is above the victim (and less when below).
     * 0 turns Drip PvP off. Read it through {@link #dripPvpPercent(GameRules)}.
     */
    public static final GameRules.Key<GameRules.IntegerValue> DRIP_PVP_PERCENT = GameRules.register(id("drip_pvp_percent"),
            GameRules.Category.PLAYER, GameRules.IntegerValue.create(0, (server, value) -> {
                int clamped = Mth.clamp(value.get(), DRIP_PVP_MIN, DRIP_PVP_MAX);
                if (clamped != value.get()) {
                    value.set(clamped, null); // null server: no second change callback
                }
            }));

    private FemboyGameRules() {
    }

    /** Registers the rules (class init); call once during common init, before any world loads. */
    public static void init() {
    }

    /** {@link #DRIP_PVP_PERCENT}, clamped to 0..50 like the 26.3 rule (a level.dat edit could bypass the command). */
    public static int dripPvpPercent(GameRules rules) {
        return Mth.clamp(rules.getInt(DRIP_PVP_PERCENT), DRIP_PVP_MIN, DRIP_PVP_MAX);
    }

    private static String id(String name) {
        return FemboyMod.MOD_ID + ":" + name;
    }

    private static GameRules.Key<GameRules.BooleanValue> bool(String name, GameRules.Category category, boolean defaultValue) {
        return GameRules.register(id(name), category, GameRules.BooleanValue.create(defaultValue));
    }
}
