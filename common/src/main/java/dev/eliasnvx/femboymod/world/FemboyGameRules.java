package dev.eliasnvx.femboymod.world;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;

/**
 * Per-world switches for server admins, changed in game with {@code /gamerule femboymod:<name> <value>}.
 * They complement the global config file: a feature runs only if both allow it.
 */
public final class FemboyGameRules {

    public static final DeferredRegister<GameRule<?>> REGISTER = DeferredRegister.create(FemboyMod.MOD_ID, Registries.GAME_RULE);

    /** Players earn Style Points. */
    public static final RegistrySupplier<GameRule<Boolean>> STYLE_POINTS = bool("style_points", GameRuleCategory.PLAYER, true);
    /** Set bonuses apply (also needs {@code set_bonuses_enabled} in the config). */
    public static final RegistrySupplier<GameRule<Boolean>> SET_BONUSES = bool("set_bonuses", GameRuleCategory.PLAYER, true);
    /** Cosmetics stay on when the player dies (like keepInventory, only for cosmetic slots). */
    public static final RegistrySupplier<GameRule<Boolean>> KEEP_COSMETICS = bool("keep_cosmetics", GameRuleCategory.DROPS, false);
    /** Players may hide their armor under the outfit; off = armor is always drawn (PvP servers). */
    public static final RegistrySupplier<GameRule<Boolean>> ALLOW_HIDDEN_ARMOR = bool("allow_hidden_armor", GameRuleCategory.PLAYER, true);
    /**
     * PvP: percent more damage per Drip tier the attacker is above the victim (and less when below).
     * 0 turns Drip PvP off.
     */
    public static final RegistrySupplier<GameRule<Integer>> DRIP_PVP_PERCENT = integer("drip_pvp_percent", GameRuleCategory.PLAYER, 0, 0, 50);

    private FemboyGameRules() {
    }

    private static RegistrySupplier<GameRule<Boolean>> bool(String name, GameRuleCategory category, boolean defaultValue) {
        return REGISTER.register(name, () -> new GameRule<>(category, GameRuleType.BOOL, BoolArgumentType.bool(),
                GameRuleTypeVisitor::visitBoolean, Codec.BOOL, value -> value ? 1 : 0, defaultValue, FeatureFlagSet.of()));
    }

    private static RegistrySupplier<GameRule<Integer>> integer(String name, GameRuleCategory category, int defaultValue, int min, int max) {
        return REGISTER.register(name, () -> new GameRule<>(category, GameRuleType.INT, IntegerArgumentType.integer(min, max),
                GameRuleTypeVisitor::visitInteger, Codec.intRange(min, max), value -> value, defaultValue, FeatureFlagSet.of()));
    }
}
