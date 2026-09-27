package dev.eliasnvx.femboymod.api.cosmetic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Data component {@code femboymod:cosmetic}: makes any item wearable in a cosmetic slot.
 *
 * <p>Attach it through {@code Item.Properties#component} using
 * {@link dev.eliasnvx.femboymod.api.FemboyDataComponents#cosmetic()}, or add it to existing items
 * with your loader's default-component modification API.
 *
 * @param slot     id of the {@link CosmeticSlotType} the item is worn in
 * @param renderer id of the renderer used to draw the item on the body; empty means the
 *                 renderer registered under the item's own id (client, Phase 2)
 */
public record Cosmetic(ResourceLocation slot, Optional<ResourceLocation> renderer) {

    /** Persistent codec. */
    public static final Codec<Cosmetic> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("slot").forGetter(Cosmetic::slot),
            ResourceLocation.CODEC.optionalFieldOf("renderer").forGetter(Cosmetic::renderer)
    ).apply(instance, Cosmetic::new));

    /** Network codec. */
    public static final StreamCodec<ByteBuf, Cosmetic> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, Cosmetic::slot,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), Cosmetic::renderer,
            Cosmetic::new);

    /**
     * Creates a cosmetic with the default renderer.
     *
     * @param slot slot id
     */
    public Cosmetic(ResourceLocation slot) {
        this(slot, Optional.empty());
    }
}
