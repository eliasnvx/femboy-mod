package dev.eliasnvx.femboymod.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.effect.CosmeticEffect;
import dev.eliasnvx.femboymod.api.effect.EffectSource;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Animal;

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
    public record FollowPassiveEffect(double radius, double speed, int interval, double stopDistance) implements CosmeticEffect {

        public static final MapCodec<FollowPassiveEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.doubleRange(1, 32).fieldOf("radius").forGetter(FollowPassiveEffect::radius),
                Codec.doubleRange(0.1, 3).optionalFieldOf("speed", 1.0).forGetter(FollowPassiveEffect::speed),
                Codec.intRange(1, 200).optionalFieldOf("interval", 10).forGetter(FollowPassiveEffect::interval),
                Codec.doubleRange(1, 16).optionalFieldOf("stop_distance", 2.5).forGetter(FollowPassiveEffect::stopDistance)
        ).apply(i, FollowPassiveEffect::new));

        @Override
        public MapCodec<FollowPassiveEffect> codec() {
            return CODEC;
        }

        @Override
        public void tick(ServerPlayer player, EffectSource source) {
            if (player.tickCount % interval != 0 || player.isSpectator()) {
                return;
            }
            double stopSq = stopDistance * stopDistance;
            for (Animal animal : player.level().getEntitiesOfClass(Animal.class, player.getBoundingBox().inflate(radius))) {
                if (!animal.isLeashed() && !animal.isVehicle() && animal.distanceToSqr(player) > stopSq) {
                    animal.getNavigation().moveTo(player, speed * source.scale());
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
}
