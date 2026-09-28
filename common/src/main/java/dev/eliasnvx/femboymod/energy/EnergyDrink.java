package dev.eliasnvx.femboymod.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * Byte Energy flavor balance (SPEC §5.2), data pack registry {@code femboymod:energy_drink}, entry id = item id:
 * {@code {"effects": [{"effect": "minecraft:speed", "duration": 1200, "amplifier": 0}]}}.
 */
public record EnergyDrink(List<Buff> effects) {

    public static final ResourceKey<Registry<EnergyDrink>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(new ResourceLocation(FemboyMod.MOD_ID, "energy_drink"));

    public static final Codec<EnergyDrink> CODEC = RecordCodecBuilder.create(i -> i.group(
            Buff.CODEC.listOf().fieldOf("effects").forGetter(EnergyDrink::effects)
    ).apply(i, EnergyDrink::new));

    public static ResourceKey<EnergyDrink> keyOf(Item item) {
        return ResourceKey.create(REGISTRY_KEY, BuiltInRegistries.ITEM.getKey(item));
    }

    public int longestDuration() {
        return effects.stream().mapToInt(Buff::duration).max().orElse(0);
    }

    /** A timed potion effect. */
    public record Buff(MobEffect effect, int duration, int amplifier) {
        public static final Codec<Buff> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.MOB_EFFECT.byNameCodec().fieldOf("effect").forGetter(Buff::effect),
                Codec.intRange(1, 20 * 60 * 60).fieldOf("duration").forGetter(Buff::duration),
                Codec.intRange(0, 255).optionalFieldOf("amplifier", 0).forGetter(Buff::amplifier)
        ).apply(i, Buff::new));

        public MobEffectInstance instance() {
            return new MobEffectInstance(effect, duration, amplifier);
        }
    }
}
