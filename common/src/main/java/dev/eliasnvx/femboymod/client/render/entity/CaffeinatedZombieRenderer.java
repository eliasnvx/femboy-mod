package dev.eliasnvx.femboymod.client.render.entity;

import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.ResourceLocation;

/**
 * Own look on the vanilla zombie model: lime skin, pink hair and sweatband, sleepless eyes with glowing
 * cyan pupils, tank top with a bolt, neon sneakers. It never stops shaking.
 */
public final class CaffeinatedZombieRenderer extends ZombieRenderer {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/caffeinated_zombie.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/caffeinated_zombie_eyes.png");

    public CaffeinatedZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayer(new EyesLayer<ZombieRenderState, ZombieModel<ZombieRenderState>>(this) {
            private final RenderType type = RenderTypes.eyes(EYES);

            @Override
            public RenderType renderType() {
                return type;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(ZombieRenderState state) {
        return TEXTURE;
    }

    @Override
    protected boolean isShaking(ZombieRenderState state) {
        return true; // jittery from caffeine; uses the vanilla conversion shake
    }
}
