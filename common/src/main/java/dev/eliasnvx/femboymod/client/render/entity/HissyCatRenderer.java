package dev.eliasnvx.femboymod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.entity.HissyCat;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Vanilla cat model with the vanilla all-black coat (referenced, not shipped) and glowing angry eyes. */
public final class HissyCatRenderer extends MobRenderer<HissyCat, HissyCatRenderer.Model> {

    private static final ResourceLocation COAT = new ResourceLocation("textures/entity/cat/all_black.png");
    private static final ResourceLocation EYES = new ResourceLocation(FemboyMod.MOD_ID, "textures/entity/hissy_cat_eyes.png");
    private static final float SHADOW = 0.4F;
    /** Vanilla CatRenderer draws cats at 80 %. */
    private static final float CAT_SCALE = 0.8F;

    public HissyCatRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModelLayers.CAT)), SHADOW);
        addLayer(new EyesLayer<>(this) {
            private final RenderType type = RenderType.eyes(EYES);

            @Override
            public RenderType renderType() {
                return type;
            }
        });
    }

    @Override
    protected void scale(HissyCat cat, PoseStack pose, float partialTick) {
        super.scale(cat, pose, partialTick);
        pose.scale(CAT_SCALE, CAT_SCALE, CAT_SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(HissyCat cat) {
        return COAT;
    }

    /** The cat model (never sitting or crouching) that takes the sprinting pose while hunting. */
    public static final class Model extends OcelotModel<HissyCat> {
        /** OcelotModel's private SPRINT_STATE. */
        private static final int SPRINT_STATE = 2;

        public Model(ModelPart root) {
            super(root);
        }

        @Override
        public void prepareMobModel(HissyCat cat, float walkPos, float walkSpeed, float partialTick) {
            super.prepareMobModel(cat, walkPos, walkSpeed, partialTick);
            if (cat.isAggressive() && !cat.isSprinting()) {
                // same adjustments as OcelotModel's sprint branch: crouched stalk while hunting
                tail2.y = tail1.y;
                tail2.z += 2.0F;
                tail1.xRot = (float) (Math.PI / 2);
                tail2.xRot = (float) (Math.PI / 2);
                state = SPRINT_STATE;
            }
        }
    }
}
