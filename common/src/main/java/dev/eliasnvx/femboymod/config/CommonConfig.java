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
                           PinkCreeper pinkCreeper, Mobs mobs, Furniture furniture, StylePoints stylePoints, VibeCheck vibeCheck, Friends friends) {

    public static final CommonConfig DEFAULTS = new CommonConfig(false, List.of(), true, PinkCreeper.DEFAULTS, Mobs.DEFAULTS,
            Furniture.DEFAULTS, StylePoints.DEFAULTS, VibeCheck.DEFAULTS, Friends.DEFAULTS);

    public static final Codec<CommonConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("keep_cosmetics_on_death").orElse(DEFAULTS.keepCosmeticsOnDeath).forGetter(CommonConfig::keepCosmeticsOnDeath),
            Identifier.CODEC.listOf().fieldOf("disabled_slots").orElse(DEFAULTS.disabledSlots).forGetter(CommonConfig::disabledSlots),
            Codec.BOOL.fieldOf("set_bonuses_enabled").orElse(DEFAULTS.setBonusesEnabled).forGetter(CommonConfig::setBonusesEnabled),
            PinkCreeper.CODEC.fieldOf("pink_creeper").orElse(PinkCreeper.DEFAULTS).forGetter(CommonConfig::pinkCreeper),
            Mobs.CODEC.fieldOf("mobs").orElse(Mobs.DEFAULTS).forGetter(CommonConfig::mobs),
            Furniture.CODEC.fieldOf("furniture").orElse(Furniture.DEFAULTS).forGetter(CommonConfig::furniture),
            StylePoints.CODEC.fieldOf("style_points").orElse(StylePoints.DEFAULTS).forGetter(CommonConfig::stylePoints),
            VibeCheck.CODEC.fieldOf("vibe_check").orElse(VibeCheck.DEFAULTS).forGetter(CommonConfig::vibeCheck),
            Friends.CODEC.fieldOf("friends").orElse(Friends.DEFAULTS).forGetter(CommonConfig::friends)
    ).apply(i, CommonConfig::new));

    public CommonConfig {
        disabledSlots = List.copyOf(disabledSlots);
    }

    /**
     * SPEC v1.2 "Friends": the stray cat and the wandering cosplayer.
     *
     * @param strayCatSpawnWeight   weight in {@code #femboymod:stray_cat_spawns} (0 disables natural spawning)
     * @param strayCatGiftChance    chance per morning that a tamed stray cat brings its owner a gift
     * @param strayCatGiftRange     how close the owner must be for the gift, in blocks
     * @param cosplayerCheckInterval ticks between attempts to send a cosplayer to a random player
     * @param cosplayerChance       chance per attempt (at most one cosplayer is around at a time)
     * @param cosplayerStayTicks    how long a cosplayer stays before leaving
     */
    public record Friends(int strayCatSpawnWeight, float strayCatGiftChance, double strayCatGiftRange,
                          int cosplayerCheckInterval, float cosplayerChance, int cosplayerStayTicks) {

        public static final Friends DEFAULTS = new Friends(6, 0.7F, 16.0, 24000, 0.25F, 36000);

        public static final Codec<Friends> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 1000).fieldOf("stray_cat_spawn_weight").orElse(DEFAULTS.strayCatSpawnWeight).forGetter(Friends::strayCatSpawnWeight),
                Codec.floatRange(0, 1).fieldOf("stray_cat_gift_chance").orElse(DEFAULTS.strayCatGiftChance).forGetter(Friends::strayCatGiftChance),
                Codec.doubleRange(1, 64).fieldOf("stray_cat_gift_range").orElse(DEFAULTS.strayCatGiftRange).forGetter(Friends::strayCatGiftRange),
                Codec.intRange(0, 720000).fieldOf("cosplayer_check_interval").orElse(DEFAULTS.cosplayerCheckInterval).forGetter(Friends::cosplayerCheckInterval),
                Codec.floatRange(0, 1).fieldOf("cosplayer_chance").orElse(DEFAULTS.cosplayerChance).forGetter(Friends::cosplayerChance),
                Codec.intRange(1200, 720000).fieldOf("cosplayer_stay_ticks").orElse(DEFAULTS.cosplayerStayTicks).forGetter(Friends::cosplayerStayTicks)
        ).apply(i, Friends::new));
    }

    /**
     * Vibe Check Scanner (SPEC v1.2): how the 0..100 score is built and what a daily scan pays.
     *
     * @param dripWeight       score per Drip Level point
     * @param perSet           score per active set bonus
     * @param maxSets          sets that count at most
     * @param collectionWeight score for a complete cosmetics collection (scaled by the share collected)
     * @param dailyPoints      Style Points for the first scan of each game day
     * @param leaderboardSize  entries kept on the server leaderboard
     */
    public record VibeCheck(float dripWeight, int perSet, int maxSets, int collectionWeight, int dailyPoints, int leaderboardSize) {

        public static final VibeCheck DEFAULTS = new VibeCheck(0.7F, 10, 2, 10, 3, 10);

        public static final Codec<VibeCheck> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0, 10).fieldOf("drip_weight").orElse(DEFAULTS.dripWeight).forGetter(VibeCheck::dripWeight),
                Codec.intRange(0, 100).fieldOf("per_set").orElse(DEFAULTS.perSet).forGetter(VibeCheck::perSet),
                Codec.intRange(0, 20).fieldOf("max_sets").orElse(DEFAULTS.maxSets).forGetter(VibeCheck::maxSets),
                Codec.intRange(0, 100).fieldOf("collection_weight").orElse(DEFAULTS.collectionWeight).forGetter(VibeCheck::collectionWeight),
                Codec.intRange(0, 1000).fieldOf("daily_points").orElse(DEFAULTS.dailyPoints).forGetter(VibeCheck::dailyPoints),
                Codec.intRange(1, 100).fieldOf("leaderboard_size").orElse(DEFAULTS.leaderboardSize).forGetter(VibeCheck::leaderboardSize)
        ).apply(i, VibeCheck::new));
    }

    /**
     * Style Points: how many are earned for what, and what a Style Coupon costs. 0 turns a source off.
     *
     * @param setWornInterval  ticks between payouts for wearing set bonuses
     * @param perActiveSet     points per active set bonus per payout
     * @param criticPassed     points when the Fashion Critic is impressed
     * @param collectionUnlock points for wearing a cosmetic item for the first time
     * @param rubberDuck       points for a debugging session with the rubber duck
     * @param couponCost       points for one Style Coupon
     */
    public record StylePoints(int setWornInterval, int perActiveSet, int criticPassed, int collectionUnlock, int rubberDuck,
                              int couponCost) {

        public static final StylePoints DEFAULTS = new StylePoints(6000, 2, 15, 5, 1, 25);

        public static final Codec<StylePoints> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 72000).fieldOf("set_worn_interval").orElse(DEFAULTS.setWornInterval).forGetter(StylePoints::setWornInterval),
                Codec.intRange(0, 1000).fieldOf("per_active_set").orElse(DEFAULTS.perActiveSet).forGetter(StylePoints::perActiveSet),
                Codec.intRange(0, 1000).fieldOf("critic_passed").orElse(DEFAULTS.criticPassed).forGetter(StylePoints::criticPassed),
                Codec.intRange(0, 1000).fieldOf("collection_unlock").orElse(DEFAULTS.collectionUnlock).forGetter(StylePoints::collectionUnlock),
                Codec.intRange(0, 1000).fieldOf("rubber_duck").orElse(DEFAULTS.rubberDuck).forGetter(StylePoints::rubberDuck),
                Codec.intRange(1, 100000).fieldOf("coupon_cost").orElse(DEFAULTS.couponCost).forGetter(StylePoints::couponCost)
        ).apply(i, StylePoints::new));
    }

    /**
     * Gamer corner furniture (SPEC v1.1 "Gamer Room").
     *
     * @param chairHealInterval ticks between heals while sitting in a Gamer Chair (0 disables the regen)
     * @param chairHealAmount   health restored per heal (half-hearts)
     * @param ledRainbow        whether LED strips offer the slow rainbow mode; off leaves only static colors
     * @param setupRadius       how far from the chair setup pieces count toward the setup rating
     * @param setupHealBonus    extra heal speed per setup star (0.25 = 5 stars heal 2.25x as often)
     * @param duckCooldown      ticks before the rubber duck grants Insight to the same player again
     * @param insightDuration   ticks of Insight from the rubber duck
     * @param duckExperience    experience points from a debugging session with the duck
     * @param terminalBtwCooldown ticks between the Terminal's chat reminders for the same player
     */
    public record Furniture(int chairHealInterval, float chairHealAmount, boolean ledRainbow, int setupRadius,
                            float setupHealBonus, int duckCooldown, int insightDuration, int duckExperience,
                            int terminalBtwCooldown) {

        public static final Furniture DEFAULTS = new Furniture(100, 1.0F, true, 4, 0.25F, 1200, 600, 3, 6000);

        public static final Codec<Furniture> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 72000).fieldOf("chair_heal_interval").orElse(DEFAULTS.chairHealInterval).forGetter(Furniture::chairHealInterval),
                Codec.floatRange(0, 20).fieldOf("chair_heal_amount").orElse(DEFAULTS.chairHealAmount).forGetter(Furniture::chairHealAmount),
                Codec.BOOL.fieldOf("led_rainbow").orElse(DEFAULTS.ledRainbow).forGetter(Furniture::ledRainbow),
                Codec.intRange(1, 8).fieldOf("setup_radius").orElse(DEFAULTS.setupRadius).forGetter(Furniture::setupRadius),
                Codec.floatRange(0, 4).fieldOf("setup_heal_bonus").orElse(DEFAULTS.setupHealBonus).forGetter(Furniture::setupHealBonus),
                Codec.intRange(0, 72000).fieldOf("duck_cooldown").orElse(DEFAULTS.duckCooldown).forGetter(Furniture::duckCooldown),
                Codec.intRange(0, 72000).fieldOf("insight_duration").orElse(DEFAULTS.insightDuration).forGetter(Furniture::insightDuration),
                Codec.intRange(0, 1000).fieldOf("duck_experience").orElse(DEFAULTS.duckExperience).forGetter(Furniture::duckExperience),
                Codec.intRange(0, 720000).fieldOf("terminal_btw_cooldown").orElse(DEFAULTS.terminalBtwCooldown).forGetter(Furniture::terminalBtwCooldown)
        ).apply(i, Furniture::new));
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
