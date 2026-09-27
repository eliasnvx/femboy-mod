package dev.eliasnvx.femboymod.client.render.entity;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.entity.HissyCat;
import net.minecraft.client.model.animal.feline.AdultCatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.ResourceLocation;

/** Vanilla cat model with the vanilla all-black coat (referenced, not shipped) and glowing angry eyes. */
public final class HissyCatRenderer extends MobRenderer<HissyCat, CatRenderState, AdultCatModel> {

    private static final ResourceLocation COAT = ResourceLocation.withDefaultNamespace("textures/entity/cat/cat_all_black.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/hissy_cat_eyes.png");
    private static final float SHADOW = 0.4F;

    public HissyCatRenderer(EntityRendererProvider.Context context) {
        super(context, new AdultCatModel(context.bakeLayer(ModelLayers.CAT)), SHADOW);
        addLayer(new EyesLayer<>(this) {
            private final RenderType type = RenderTypes.eyes(EYES);

            @Override
            public RenderType renderType() {
                return type;
            }
        });
    }

    @Override
    public CatRenderState createRenderState() {
        return new CatRenderState();
    }

    @Override
    public void extractRenderState(HissyCat cat, CatRenderState state, float partialTick) {
        super.extractRenderState(cat, state, partialTick);
        state.texture = COAT;
        state.isSprinting = cat.isAggressive(); // crouched stalk while hunting
        state.isCrouching = false;
        state.isSitting = false;
    }

    @Override
    public ResourceLocation getTextureLocation(CatRenderState state) {
        return COAT;
    }
}
