package dev.eliasnvx.femboymod.mixin.client;

import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Pet outfits sit on the head of cat and wolf models, which keep it to themselves on 1.21.1 (no {@code root()}), and
 * kittens and puppies draw that head scaled and moved ({@code AgeableListModel#renderToBuffer}); the outfit layer
 * repeats the same transform (client/render/pet/PetOutfitLayer). 26.x has separate baby models instead.
 */
@Mixin(AgeableListModel.class)
public interface AgeableListModelAccessor {

    @Invoker("headParts")
    Iterable<ModelPart> femboymod$headParts();

    @Accessor("scaleHead")
    boolean femboymod$scaleHead();

    @Accessor("babyYHeadOffset")
    float femboymod$babyYHeadOffset();

    @Accessor("babyZHeadOffset")
    float femboymod$babyZHeadOffset();

    @Accessor("babyHeadScale")
    float femboymod$babyHeadScale();
}
