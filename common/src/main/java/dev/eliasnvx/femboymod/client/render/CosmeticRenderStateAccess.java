package dev.eliasnvx.femboymod.client.render;

import org.jetbrains.annotations.Nullable;

/** Duck interface mixed into AvatarRenderState. */
public interface CosmeticRenderStateAccess {

    @Nullable CosmeticRenderData femboymod$getCosmetics();

    void femboymod$setCosmetics(@Nullable CosmeticRenderData data);
}
