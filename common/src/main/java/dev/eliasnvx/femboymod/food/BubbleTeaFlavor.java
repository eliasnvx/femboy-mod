package dev.eliasnvx.femboymod.food;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.energy.EnergyDrink;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;

/**
 * A bubble tea flavor (SPEC v1.1), data pack registry {@code femboymod:bubble_tea_flavor}. Every drink picks a
 * random flavor by weight: {@code {"weight": 3, "effects": [{"effect": "minecraft:haste", "duration": 1200}]}}.
 * The flavor's name is the lang key {@code bubble_tea_flavor.<namespace>.<path>}.
 */
public record BubbleTeaFlavor(int weight, List<EnergyDrink.Buff> effects) {

    public static final ResourceKey<Registry<BubbleTeaFlavor>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "bubble_tea_flavor"));

    public static final Codec<BubbleTeaFlavor> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 1000).optionalFieldOf("weight", 1).forGetter(BubbleTeaFlavor::weight),
            EnergyDrink.Buff.CODEC.listOf().fieldOf("effects").forGetter(BubbleTeaFlavor::effects)
    ).apply(i, BubbleTeaFlavor::new));
}
