package dev.eliasnvx.femboymod.client.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.cosmetic.Colorways;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Item model tint {@code femboymod:colorway}: colors a (grayscale) icon layer with the item's colorway.
 * {@code {"type": "femboymod:colorway", "default": 16094654, "stripe": 1}}; {@code stripe} picks which
 * stripe of a pattern the layer shows (wraps), {@code default} is used for undyed items.
 */
public record ColorwayTintSource(int defaultColor, int stripe) implements ItemTintSource {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "colorway");

    public static final MapCodec<ColorwayTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(ColorwayTintSource::defaultColor),
            Codec.intRange(0, 64).optionalFieldOf("stripe", 0).forGetter(ColorwayTintSource::stripe)
    ).apply(i, ColorwayTintSource::new));

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        Colorway colorway = Colorways.effective(stack).orElse(null);
        return ARGB.opaque(colorway == null ? defaultColor : colorway.stripeColor(stripe, ColorwayClock.ticks()));
    }

    @Override
    public MapCodec<ColorwayTintSource> type() {
        return MAP_CODEC;
    }
}
