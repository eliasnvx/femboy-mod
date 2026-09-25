package dev.eliasnvx.femboymod.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server/common options ({@code config/femboymod-common.json}, SPEC §10). Balance of items, sets, drip
 * and drinks lives in data packs; this file holds switches and the Pink Creeper numbers.
 */
public record CommonConfig(boolean keepCosmeticsOnDeath, List<Identifier> disabledSlots, boolean setBonusesEnabled,
                           PinkCreeper pinkCreeper) {

    public static final CommonConfig DEFAULTS = new CommonConfig(false, List.of(), true, PinkCreeper.DEFAULTS);

    public static final Codec<CommonConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("keep_cosmetics_on_death").orElse(DEFAULTS.keepCosmeticsOnDeath).forGetter(CommonConfig::keepCosmeticsOnDeath),
            Identifier.CODEC.listOf().fieldOf("disabled_slots").orElse(DEFAULTS.disabledSlots).forGetter(CommonConfig::disabledSlots),
            Codec.BOOL.fieldOf("set_bonuses_enabled").orElse(DEFAULTS.setBonusesEnabled).forGetter(CommonConfig::setBonusesEnabled),
            PinkCreeper.CODEC.fieldOf("pink_creeper").orElse(PinkCreeper.DEFAULTS).forGetter(CommonConfig::pinkCreeper)
    ).apply(i, CommonConfig::new));

    public CommonConfig {
        disabledSlots = List.copyOf(disabledSlots);
    }

    /**
     * @param spawnWeight   spawn weight in {@code #femboymod:pink_creeper_spawns} biomes (0 disables spawning)
     * @param playerDamage  damage of the confetti blast to players (SPEC: 0)
     * @param mobDamage     damage to other mobs
     * @param knockback     knockback strength
     * @param radius        blast radius in blocks; blocks are never destroyed
     */
    public record PinkCreeper(int spawnWeight, int minGroup, int maxGroup, float playerDamage, float mobDamage,
                              float knockback, float radius) {

        public static final PinkCreeper DEFAULTS = new PinkCreeper(8, 1, 1, 0.0F, 0.0F, 1.2F, 3.5F);

        public static final Codec<PinkCreeper> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 1000).fieldOf("spawn_weight").orElse(DEFAULTS.spawnWeight).forGetter(PinkCreeper::spawnWeight),
                Codec.intRange(1, 8).fieldOf("min_group").orElse(DEFAULTS.minGroup).forGetter(PinkCreeper::minGroup),
                Codec.intRange(1, 8).fieldOf("max_group").orElse(DEFAULTS.maxGroup).forGetter(PinkCreeper::maxGroup),
                Codec.floatRange(0, 100).fieldOf("player_damage").orElse(DEFAULTS.playerDamage).forGetter(PinkCreeper::playerDamage),
                Codec.floatRange(0, 100).fieldOf("mob_damage").orElse(DEFAULTS.mobDamage).forGetter(PinkCreeper::mobDamage),
                Codec.floatRange(0, 10).fieldOf("knockback").orElse(DEFAULTS.knockback).forGetter(PinkCreeper::knockback),
                Codec.floatRange(0.5F, 16).fieldOf("radius").orElse(DEFAULTS.radius).forGetter(PinkCreeper::radius)
        ).apply(i, PinkCreeper::new));
    }
}
