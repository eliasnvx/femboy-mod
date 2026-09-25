package dev.eliasnvx.femboymod.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Everything a {@link CosmeticRenderer} needs for one submission. A reused object: do not keep it. */
public interface CosmeticRenderContext {

    /** @return pose stack, already in player model space */
    PoseStack poseStack();

    /** @return the submit collector */
    SubmitNodeCollector collector();

    /** @return packed light */
    int light();

    /** @return packed overlay (hurt flash) */
    int overlay();

    /** @return the player's render state; pass it to {@code submitModel} */
    AvatarRenderState state();

    /** @return the player's model, posed for this player at submit time only */
    PlayerModel parentModel();

    /** @return the worn item; do not modify */
    ItemStack stack();

    /** @return the slot it is worn in */
    Identifier slot();

    /** @return its colorway (including vanilla dyeing), or null if undyed */
    @Nullable Colorway colorway();

    /** @return motion values for procedural animation of this player */
    CosmeticMotion motion();
}
