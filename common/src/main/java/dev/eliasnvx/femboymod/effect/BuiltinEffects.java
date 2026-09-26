package dev.eliasnvx.femboymod.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.effect.CosmeticEffect;
import dev.eliasnvx.femboymod.api.effect.EffectSource;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import dev.eliasnvx.femboymod.combat.DripCombat;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Animal;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Built-in {@link CosmeticEffect} types. All numbers come from JSON. */
public final class BuiltinEffects {

    private BuiltinEffects() {
    }

    public static void register(ApiRegistry<MapCodec<? extends CosmeticEffect>> registry) {
        registry.register(id("attribute"), AttributeEffect.CODEC);
        registry.register(id("mob_effect"), MobEffectEffect.CODEC);
        registry.register(id("particles"), ParticlesEffect.CODEC);
        registry.register(id("step_sound"), StepSoundEffect.CODEC);
        registry.register(id("follow_passive"), FollowPassiveEffect.CODEC);
        registry.register(id("glow_hostiles"), GlowHostilesEffect.CODEC);
        registry.register(id("damage_bonus"), DamageBonusEffect.CODEC);
        registry.register(id("glow_friends"), GlowFriendsEffect.CODEC);
        registry.register(id("muffle_sounds"), MuffleSoundsEffect.CODEC);
    }

    /**
     * Player's melee and projectile damage against {@code targets} is multiplied by {@code multiplier}
     * (the part above 1 scales with the source, e.g. set bonus tiers). Applied by {@link DripCombat}.
     */
    public record DamageBonusEffect(HolderSet<EntityType<?>> targets, float multiplier) implements CosmeticEffect {

        public static final MapCodec<DamageBonusEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                RegistryCodecs.holderSet(Registries.ENTITY_TYPE).fieldOf("targets").forGetter(DamageBonusEffect::targets),
                Codec.floatRange(0.0F, 10.0F).fieldOf("multiplier").forGetter(DamageBonusEffect::multiplier)
        ).apply(i, DamageBonusEffect::new));

        @Override
        public MapCodec<DamageBonusEffect> codec() {
            return CODEC;
        }

        @Override
        public void onActivate(ServerPlayer player, EffectSource source) {
            DripCombat.addBonus(player, source.id(), this, source.scale());
        }

        @Override
        public void onDeactivate(ServerPlayer player, EffectSource source) {
            DripCombat.removeBonus(player, source.id());
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, path);
    }

    /** Transient attribute modifier; amount is multiplied by the source scale. Id = source id. */
    public record AttributeEffect(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation)
            implements CosmeticEffect {

        public static final MapCodec<AttributeEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Attribute.CODEC.fieldOf("attribute").forGetter(AttributeEffect::attribute),
                Codec.DOUBLE.fieldOf("amount").forGetter(AttributeEffect::amount),
                AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(AttributeEffect::operation)
        ).apply(i, AttributeEffect::new));

        @Override
        public MapCodec<AttributeEffect> codec() {
            return CODEC;
        }

        @Override
        public void onActivate(ServerPlayer player, EffectSource source) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(source.id());
                instance.addTransientModifier(new AttributeModifier(source.id(), amount * source.scale(), operation));
            }
        }

        @Override
        public void onDeactivate(ServerPlayer player, EffectSource source) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(source.id());
            }
        }
    }

    /** Keeps a potion effect topped up while active; it simply runs out after deactivation. */
    public record MobEffectEffect(Holder<MobEffect> effect, int amplifier, int duration, boolean showIcon) implements CosmeticEffect {

        public static final MapCodec<MobEffectEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                MobEffect.CODEC.fieldOf("effect").forGetter(MobEffectEffect::effect),
                Codec.intRange(0, 255).optionalFieldOf("amplifier", 0).forGetter(MobEffectEffect::amplifier),
                Codec.intRange(2, 20 * 60).optionalFieldOf("duration", 60).forGetter(MobEffectEffect::duration),
                Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(MobEffectEffect::showIcon)
        ).apply(i, MobEffectEffect::new));

        @Override
        public MapCodec<MobEffectEffect> codec() {
            return CODEC;
        }

        @Override
        public void tick(ServerPlayer player, EffectSource source) {
            MobEffectInstance current = player.getEffect(effect);
            if (current == null || current.getAmplifier() < amplifier || current.getDuration() < duration / 2) {
                player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, showIcon));
            }
        }
    }

    /** Spawns particles around the wearer every {@code interval} ticks. */
    public record ParticlesEffect(ParticleOptions particle, int interval, int count, double spread) implements CosmeticEffect {

        public static final MapCodec<ParticlesEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ParticleTypes.CODEC.fieldOf("particle").forGetter(ParticlesEffect::particle),
                Codec.intRange(1, 20 * 60).fieldOf("interval").forGetter(ParticlesEffect::interval),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(ParticlesEffect::count),
                Codec.doubleRange(0, 8).optionalFieldOf("spread", 0.5).forGetter(ParticlesEffect::spread)
        ).apply(i, ParticlesEffect::new));

        @Override
        public MapCodec<ParticlesEffect> codec() {
            return CODEC;
        }

        @Override
        public void tick(ServerPlayer player, EffectSource source) {
            if (player.tickCount % interval == 0 && !player.isInvisible()) {
                ((ServerLevel) player.level()).sendParticles(particle, player.getX(), player.getY() + player.getBbHeight() * 0.75,
                        player.getZ(), count, spread, spread * 0.5, spread, 0.0);
            }
        }
    }

    /** Plays a sound every {@code distance} blocks walked (the UwU choker's bell). */
    public record StepSoundEffect(Holder<SoundEvent> sound, float distance, float volume, float pitch) implements CosmeticEffect {

        public static final MapCodec<StepSoundEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                SoundEvent.CODEC.fieldOf("sound").forGetter(StepSoundEffect::sound),
                Codec.floatRange(0.1F, 64F).fieldOf("distance").forGetter(StepSoundEffect::distance),
                Codec.floatRange(0F, 4F).optionalFieldOf("volume", 1F).forGetter(StepSoundEffect::volume),
                Codec.floatRange(0.1F, 4F).optionalFieldOf("pitch", 1F).forGetter(StepSoundEffect::pitch)
        ).apply(i, StepSoundEffect::new));

        /** moveDist at the last jingle, per player. */
        private static final Map<ServerPlayer, Float> LAST = new WeakHashMap<>();

        @Override
        public MapCodec<StepSoundEffect> codec() {
            return CODEC;
        }

        @Override
        public void onActivate(ServerPlayer player, EffectSource source) {
            LAST.put(player, player.moveDist);
        }

        @Override
        public void tick(ServerPlayer player, EffectSource source) {
            float last = LAST.getOrDefault(player, player.moveDist);
            if (!player.onGround() || player.isPassenger() || player.isShiftKeyDown()) {
                LAST.put(player, player.moveDist);
                return;
            }
            if (player.moveDist - last >= distance) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
                LAST.put(player, player.moveDist);
            }
        }

        @Override
        public void onDeactivate(ServerPlayer player, EffectSource source) {
            LAST.remove(player);
        }
    }

    /** Passive animals nearby walk after the wearer, like players holding wheat. */
    /**
     * Small cute animals ({@code followers}, default {@code #femboymod:cute_followers}) walk after the player.
     * At most {@code max_followers} of the nearest ones, never while the player rides something.
     */
    public record FollowPassiveEffect(double radius, double speed, int interval, double stopDistance,
                                      HolderSet<EntityType<?>> followers, int maxFollowers) implements CosmeticEffect {

        private static final HolderSet<EntityType<?>> NO_TAG = HolderSet.empty();

        public static final MapCodec<FollowPassiveEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.doubleRange(1, 32).fieldOf("radius").forGetter(FollowPassiveEffect::radius),
                Codec.doubleRange(0.1, 3).optionalFieldOf("speed", 1.0).forGetter(FollowPassiveEffect::speed),
                Codec.intRange(1, 200).optionalFieldOf("interval", 10).forGetter(FollowPassiveEffect::interval),
                Codec.doubleRange(1, 16).optionalFieldOf("stop_distance", 2.5).forGetter(FollowPassiveEffect::stopDistance),
                RegistryCodecs.holderSet(Registries.ENTITY_TYPE).optionalFieldOf("followers", NO_TAG).forGetter(FollowPassiveEffect::followers),
                Codec.intRange(0, 16).optionalFieldOf("max_followers", 3).forGetter(FollowPassiveEffect::maxFollowers)
        ).apply(i, FollowPassiveEffect::new));

        @Override
        public MapCodec<FollowPassiveEffect> codec() {
            return CODEC;
        }

        private boolean follows(Animal animal) {
            return followers.size() > 0
                    ? followers.contains(animal.getType().builtInRegistryHolder())
                    : animal.getType().builtInRegistryHolder().is(FemboyTags.CUTE_FOLLOWERS);
        }

        /** The nearest {@code maxFollowers} free cute animals in range. */
        public List<Animal> pickFollowers(ServerPlayer player) {
            List<Animal> nearby = player.level().getEntitiesOfClass(Animal.class, player.getBoundingBox().inflate(radius),
                    animal -> follows(animal) && !animal.isLeashed() && !animal.isVehicle() && !animal.isPassenger());
            nearby.sort(Comparator.comparingDouble(animal -> animal.distanceToSqr(player)));
            return nearby.subList(0, Math.min(maxFollowers, nearby.size()));
        }

        @Override
        public void tick(ServerPlayer player, EffectSource source) {
            if (player.tickCount % interval != 0 || player.isSpectator() || player.isPassenger() || maxFollowers == 0) {
                return;
            }
            double stopSq = stopDistance * stopDistance;
            for (Animal animal : pickFollowers(player)) {
                if (animal.distanceToSqr(player) > stopSq) {
                    animal.getNavigation().moveTo(player, speed * source.scale());
                } else {
                    animal.getNavigation().stop(); // close enough: don't push into the player
                }
            }
        }
    }

    /** Client-side only: hostile mobs within {@code radius} glow for the wearer (see GlowHostilesClient). */
    public record GlowHostilesEffect(double radius) implements CosmeticEffect {

        public static final MapCodec<GlowHostilesEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.doubleRange(1, 64).fieldOf("radius").forGetter(GlowHostilesEffect::radius)
        ).apply(i, GlowHostilesEffect::new));

        @Override
        public MapCodec<GlowHostilesEffect> codec() {
            return CODEC;
        }
    }

    /**
     * Client-side only: other players within {@code radius} glow for the wearer. With {@code teammates_only},
     * only players on the wearer's scoreboard team (everyone, if the wearer has no team).
     */
    public record GlowFriendsEffect(double radius, boolean teammatesOnly) implements CosmeticEffect {

        public static final MapCodec<GlowFriendsEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.doubleRange(1, 64).fieldOf("radius").forGetter(GlowFriendsEffect::radius),
                Codec.BOOL.optionalFieldOf("teammates_only", true).forGetter(GlowFriendsEffect::teammatesOnly)
        ).apply(i, GlowFriendsEffect::new));

        @Override
        public MapCodec<GlowFriendsEffect> codec() {
            return CODEC;
        }
    }

    /** Client-side only: the listed sounds play at {@code volume} (0..1) for the wearer. */
    public record MuffleSoundsEffect(java.util.List<Identifier> sounds, float volume) implements CosmeticEffect {

        public static final MapCodec<MuffleSoundsEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.listOf().fieldOf("sounds").forGetter(MuffleSoundsEffect::sounds),
                Codec.floatRange(0, 1).fieldOf("volume").forGetter(MuffleSoundsEffect::volume)
        ).apply(i, MuffleSoundsEffect::new));

        public MuffleSoundsEffect {
            sounds = java.util.List.copyOf(sounds);
        }

        @Override
        public MapCodec<MuffleSoundsEffect> codec() {
            return CODEC;
        }
    }
}
