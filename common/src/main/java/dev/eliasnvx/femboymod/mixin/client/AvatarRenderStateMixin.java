package dev.eliasnvx.femboymod.mixin.client;

import dev.eliasnvx.femboymod.client.render.CosmeticRenderData;
import dev.eliasnvx.femboymod.client.render.CosmeticRenderStateAccess;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements CosmeticRenderStateAccess {

    @Unique
    private @Nullable CosmeticRenderData femboymod$cosmetics;

    @Override
    public @Nullable CosmeticRenderData femboymod$getCosmetics() {
        return femboymod$cosmetics;
    }

    @Override
    public void femboymod$setCosmetics(@Nullable CosmeticRenderData data) {
        femboymod$cosmetics = data;
    }
}
