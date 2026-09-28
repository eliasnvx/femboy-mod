package dev.eliasnvx.femboymod.api.cosmetic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
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

    /**
     * Writes this cosmetic to a packet.
     *
     * @param buf the packet buffer
     */
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(slot);
        buf.writeOptional(renderer, FriendlyByteBuf::writeResourceLocation);
    }

    /**
     * Reads a cosmetic written by {@link #write}.
     *
     * @param buf the packet buffer
     * @return the cosmetic
     */
    public static Cosmetic read(FriendlyByteBuf buf) {
        return new Cosmetic(buf.readResourceLocation(), buf.readOptional(FriendlyByteBuf::readResourceLocation));
    }

    /**
     * Creates a cosmetic with the default renderer.
     *
     * @param slot slot id
     */
    public Cosmetic(ResourceLocation slot) {
        this(slot, Optional.empty());
    }
}
