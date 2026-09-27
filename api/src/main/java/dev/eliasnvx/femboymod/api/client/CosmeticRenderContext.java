package dev.eliasnvx.femboymod.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Everything a {@link CosmeticRenderer} needs for one submission. A reused object: do not keep it. */
public interface CosmeticRenderContext {

    /** @return pose stack, already in player model space */
    PoseStack poseStack();

    /** @return the buffer source to draw into */
    MultiBufferSource bufferSource();

    /** @return packed light */
    int light();

    /** @return packed overlay (hurt flash) */
    int overlay();

    /** @return the entity wearing the cosmetic (a player, or a mob that renders cosmetics) */
    LivingEntity entity();

    /** @return the partial tick of this frame */
    float partialTick();

    /** @return the wearer's model, posed for this entity for this frame only */
    HumanoidModel<?> parentModel();

    /** @return the worn item; do not modify */
    ItemStack stack();

    /** @return the slot it is worn in */
    ResourceLocation slot();

    /** @return its colorway (including vanilla dyeing), or null if undyed */
    @Nullable Colorway colorway();

    /** @return motion values for procedural animation of this player */
    CosmeticMotion motion();
}
