package dev.eliasnvx.femboymod.api.drip;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.api.FemboyApi;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;

import java.util.List;

/**
 * Global Drip Level tuning, loaded from the data pack registry {@code femboymod:drip_rules};
 * the active entry is {@link #DEFAULT} ({@code data/femboymod/femboymod/drip_rules/default.json}).
 *
 * <p>Drip Level = sum of worn items' {@code drip} + {@code harmony_bonus} per item that shares
 * its colorway with another worn item + {@code set_bonus} per completed set, clamped to
 * {@code 0..max_level}. The tier is the number of {@code tier_thresholds} the level reaches.
 *
 * @param harmonyBonus   bonus per color-matched item
 * @param setBonus       bonus per completed set
 * @param maxLevel       upper clamp (SPEC: 100)
 * @param tierThresholds ascending levels for tiers 1..N (SPEC: tiers 0–5)
 */
public record DripRules(int harmonyBonus, int setBonus, int maxLevel, List<Integer> tierThresholds) {

    /** Registry key of the {@code femboymod:drip_rules} data pack registry. */
    public static final ResourceKey<Registry<DripRules>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(new ResourceLocation(FemboyApi.MOD_ID, "drip_rules"));

    /** The entry that is used: {@code femboymod:default}. Data packs override it to rebalance. */
    public static final ResourceKey<DripRules> DEFAULT =
            ResourceKey.create(REGISTRY_KEY, new ResourceLocation(FemboyApi.MOD_ID, "default"));

    /** Rules used when the data pack entry is missing: no bonuses, one tier. */
    public static final DripRules FALLBACK = new DripRules(0, 0, 100, List.of());

    /** JSON codec. */
    public static final Codec<DripRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 1000).fieldOf("harmony_bonus").forGetter(DripRules::harmonyBonus),
            Codec.intRange(0, 1000).fieldOf("set_bonus").forGetter(DripRules::setBonus),
            Codec.intRange(1, 10000).fieldOf("max_level").forGetter(DripRules::maxLevel),
            Codec.intRange(0, 10000).listOf().fieldOf("tier_thresholds").forGetter(DripRules::tierThresholds)
    ).apply(instance, DripRules::new));

    /** @throws IllegalArgumentException if thresholds are not ascending */
    public DripRules {
        tierThresholds = List.copyOf(tierThresholds);
        for (int i = 1; i < tierThresholds.size(); i++) {
            if (tierThresholds.get(i) < tierThresholds.get(i - 1)) {
                throw new IllegalArgumentException("tier_thresholds must be ascending: " + tierThresholds);
            }
        }
    }

    /**
     * Returns the tier for a level.
     *
     * @param level drip level
     * @return 0 when below the first threshold, up to {@code tierThresholds().size()}
     */
    public int tierOf(int level) {
        int tier = 0;
        for (int threshold : tierThresholds) {
            if (level >= threshold) {
                tier++;
            }
        }
        return tier;
    }
}
