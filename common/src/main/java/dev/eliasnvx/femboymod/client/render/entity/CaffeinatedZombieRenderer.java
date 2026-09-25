package dev.eliasnvx.femboymod.client.render.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;

/** Vanilla zombie look (the Byte Energy can in hand tells it apart) that never stops shaking. */
public final class CaffeinatedZombieRenderer extends ZombieRenderer {

    public CaffeinatedZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected boolean isShaking(ZombieRenderState state) {
        return true; // jittery from caffeine; uses the vanilla conversion shake
    }
}
