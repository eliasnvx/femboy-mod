package dev.eliasnvx.femboymod.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;

import java.util.List;

/**
 * Crash and jitter tuning (SPEC §5.2), data pack registry {@code femboymod:caffeine}; entry {@code femboymod:default}.
 *
 * @param crash         effects applied when the caffeine wears off
 * @param jitterWindow  window in ticks for counting cans
 * @param jitterAfter   more than this many cans within the window causes jitter
 * @param jitter        the jitter effect(s); must not flash the screen (photosensitivity)
 */
public record CaffeineRules(List<EnergyDrink.Buff> crash, int jitterWindow, int jitterAfter, List<EnergyDrink.Buff> jitter) {

    public static final ResourceKey<Registry<CaffeineRules>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "caffeine"));
    public static final ResourceKey<CaffeineRules> DEFAULT =
            ResourceKey.create(REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "default"));
    public static final CaffeineRules FALLBACK = new CaffeineRules(List.of(), 1, Integer.MAX_VALUE, List.of());

    public static final Codec<CaffeineRules> CODEC = RecordCodecBuilder.create(i -> i.group(
            EnergyDrink.Buff.CODEC.listOf().optionalFieldOf("crash", List.of()).forGetter(CaffeineRules::crash),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("jitter_window").forGetter(CaffeineRules::jitterWindow),
            Codec.intRange(0, 1000).fieldOf("jitter_after").forGetter(CaffeineRules::jitterAfter),
            EnergyDrink.Buff.CODEC.listOf().optionalFieldOf("jitter", List.of()).forGetter(CaffeineRules::jitter)
    ).apply(i, CaffeineRules::new));
}
