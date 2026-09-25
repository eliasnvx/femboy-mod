package dev.eliasnvx.femboymod.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server/common options ({@code config/femboymod-common.json}, SPEC §10). Balance of items, sets, drip
 * and drinks lives in data packs; this file holds switches, the Pink Creeper numbers and hostile mob spawning/stats.
 */
public record CommonConfig(boolean keepCosmeticsOnDeath, List<Identifier> disabledSlots, boolean setBonusesEnabled,
                           PinkCreeper pinkCreeper, Mobs mobs) {

    public static final CommonConfig DEFAULTS = new CommonConfig(false, List.of(), true, PinkCreeper.DEFAULTS, Mobs.DEFAULTS);

    public static final Codec<CommonConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("keep_cosmetics_on_death").orElse(DEFAULTS.keepCosmeticsOnDeath).forGetter(CommonConfig::keepCosmeticsOnDeath),
            Identifier.CODEC.listOf().fieldOf("disabled_slots").orElse(DEFAULTS.disabledSlots).forGetter(CommonConfig::disabledSlots),
            Codec.BOOL.fieldOf("set_bonuses_enabled").orElse(DEFAULTS.setBonusesEnabled).forGetter(CommonConfig::setBonusesEnabled),
            PinkCreeper.CODEC.fieldOf("pink_creeper").orElse(PinkCreeper.DEFAULTS).forGetter(CommonConfig::pinkCreeper),
            Mobs.CODEC.fieldOf("mobs").orElse(Mobs.DEFAULTS).forGetter(CommonConfig::mobs)
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

    /**
     * Spawning and base stats of a hostile mob. How hard it hits a stylish player is data-driven
     * ({@code femboymod:drip_damage}).
     *
     * @param spawnWeight  weight in the mob's spawn biome tag (0 disables natural spawning)
     * @param health       max health (half-hearts)
     * @param attackDamage melee damage (half-hearts)
     * @param speed        movement speed attribute
     */
    public record Mob(int spawnWeight, int minGroup, int maxGroup, double health, double attackDamage, double speed) {

        public static Codec<Mob> codec(Mob defaults) {
            return RecordCodecBuilder.create(i -> i.group(
                    Codec.intRange(0, 1000).fieldOf("spawn_weight").orElse(defaults.spawnWeight).forGetter(Mob::spawnWeight),
                    Codec.intRange(1, 16).fieldOf("min_group").orElse(defaults.minGroup).forGetter(Mob::minGroup),
                    Codec.intRange(1, 16).fieldOf("max_group").orElse(defaults.maxGroup).forGetter(Mob::maxGroup),
                    Codec.doubleRange(1, 1024).fieldOf("health").orElse(defaults.health).forGetter(Mob::health),
                    Codec.doubleRange(0, 100).fieldOf("attack_damage").orElse(defaults.attackDamage).forGetter(Mob::attackDamage),
                    Codec.doubleRange(0, 2).fieldOf("speed").orElse(defaults.speed).forGetter(Mob::speed)
            ).apply(i, Mob::new));
        }
    }

    /** Hostile meme mobs: bugs, the caffeinated zombie, the hissy cat and the Fashion Critic mini-boss. */
    public record Mobs(Mob bug, Mob caffeinatedZombie, Mob hissyCat, Mob fashionCritic, CriticReview criticReview) {

        public static final Mobs DEFAULTS = new Mobs(
                new Mob(40, 3, 5, 6.0, 2.0, 0.32),
                new Mob(20, 1, 2, 20.0, 3.0, 0.3),
                new Mob(15, 1, 1, 10.0, 3.0, 0.38),
                new Mob(1, 1, 1, 80.0, 7.0, 0.3),
                CriticReview.DEFAULTS);

        public static final Codec<Mobs> CODEC = RecordCodecBuilder.create(i -> i.group(
                Mob.codec(DEFAULTS.bug).fieldOf("bug").orElse(DEFAULTS.bug).forGetter(Mobs::bug),
                Mob.codec(DEFAULTS.caffeinatedZombie).fieldOf("caffeinated_zombie").orElse(DEFAULTS.caffeinatedZombie).forGetter(Mobs::caffeinatedZombie),
                Mob.codec(DEFAULTS.hissyCat).fieldOf("hissy_cat").orElse(DEFAULTS.hissyCat).forGetter(Mobs::hissyCat),
                Mob.codec(DEFAULTS.fashionCritic).fieldOf("fashion_critic").orElse(DEFAULTS.fashionCritic).forGetter(Mobs::fashionCritic),
                CriticReview.CODEC.fieldOf("critic_review").orElse(DEFAULTS.criticReview).forGetter(Mobs::criticReview)
        ).apply(i, Mobs::new));
    }

    /**
     * The Fashion Critic's "review": every {@code cooldownTicks} it judges its target within {@code range}.
     * Drip tier below {@code impressedTier}: slowness + weakness on the player; otherwise the critic is
     * impressed and gets weakness itself. Effects last {@code effectTicks}.
     */
    public record CriticReview(int cooldownTicks, double range, int impressedTier, int effectTicks, int slownessLevel) {

        public static final CriticReview DEFAULTS = new CriticReview(160, 12.0, 3, 100, 1);

        public static final Codec<CriticReview> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(20, 12000).fieldOf("cooldown_ticks").orElse(DEFAULTS.cooldownTicks).forGetter(CriticReview::cooldownTicks),
                Codec.doubleRange(1, 64).fieldOf("range").orElse(DEFAULTS.range).forGetter(CriticReview::range),
                Codec.intRange(0, 32).fieldOf("impressed_tier").orElse(DEFAULTS.impressedTier).forGetter(CriticReview::impressedTier),
                Codec.intRange(1, 12000).fieldOf("effect_ticks").orElse(DEFAULTS.effectTicks).forGetter(CriticReview::effectTicks),
                Codec.intRange(0, 4).fieldOf("slowness_level").orElse(DEFAULTS.slownessLevel).forGetter(CriticReview::slownessLevel)
        ).apply(i, CriticReview::new));
    }
}
