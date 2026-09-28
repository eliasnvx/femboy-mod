package dev.eliasnvx.femboymod.client.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.cosmetic.Colorways;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Item tint {@code femboymod:colorway}: colors a (grayscale) icon layer with the item's colorway.
 * {@code {"type": "femboymod:colorway", "default": 16094654, "stripe": 1}}; {@code stripe} picks which
 * stripe of a pattern the layer shows (wraps), {@code default} is used for undyed items.
 *
 * <p>1.20.1 has no data-driven item tint sources: {@code client.ItemTints} reads these entries from
 * {@code assets/<ns>/items/*.json} and serves them through an {@code ItemColor}. This record is the shared
 * definition (id, codec, color math) for that and for addons.
 */
public record ColorwayTintSource(int defaultColor, int stripe) {

    public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "colorway");

    private static final int MAX_STRIPE = 64;

    public static final MapCodec<ColorwayTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("default").forGetter(ColorwayTintSource::defaultColor),
            Codec.intRange(0, MAX_STRIPE).optionalFieldOf("stripe", 0).forGetter(ColorwayTintSource::stripe)
    ).apply(i, ColorwayTintSource::new));

    /** @return the layer color (opaque ARGB): the colorway's stripe, or the default color while undyed */
    public int calculate(ItemStack stack) {
        Colorway colorway = Colorways.effective(stack).orElse(null);
        return ModelColors.opaque(colorway == null ? defaultColor : colorway.stripeColor(stripe, ColorwayClock.ticks()));
    }
}
