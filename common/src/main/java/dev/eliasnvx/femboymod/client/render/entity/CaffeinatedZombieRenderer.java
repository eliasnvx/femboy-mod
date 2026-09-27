package dev.eliasnvx.femboymod.client.render.entity;

import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Own look on the vanilla zombie model: lime skin, pink hair and sweatband, sleepless eyes with glowing
 * cyan pupils, tank top with a bolt, neon sneakers. It never stops shaking.
 */
public final class CaffeinatedZombieRenderer extends ZombieRenderer {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/caffeinated_zombie.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/caffeinated_zombie_eyes.png");

    public CaffeinatedZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayer(new EyesLayer<Zombie, ZombieModel<Zombie>>(this) {
            private final RenderType type = RenderType.eyes(EYES);

            @Override
            public RenderType renderType() {
                return type;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie zombie) {
        return TEXTURE;
    }

    @Override
    protected boolean isShaking(Zombie zombie) {
        return true; // jittery from caffeine; uses the vanilla conversion shake
    }
}
