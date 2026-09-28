package dev.eliasnvx.femboymod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.monster.Creeper;

/** Pink Creeper: vanilla creeper model with our own pink texture and a bow on the head (SPEC §5.4). */
public final class PinkCreeperRenderer extends CreeperRenderer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(FemboyMod.MOD_ID, "textures/entity/pink_creeper.png");
    public static final ModelLayerLocation BOW = new ModelLayerLocation(new ResourceLocation(FemboyMod.MOD_ID, "pink_creeper_bow"), "main");

    public PinkCreeperRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayer(new BowLayer(this, context.bakeLayer(BOW)));
    }

    @Override
    public ResourceLocation getTextureLocation(Creeper creeper) {
        return TEXTURE;
    }

    public static void registerLayers() {
        EntityModelLayerRegistry.register(BOW, PinkCreeperRenderer::bow);
    }

    /** A bow sitting on top of the head, slightly to the side. */
    private static LayerDefinition bow() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bow = root.addOrReplaceChild("bow", CubeListBuilder.create(), PartPose.offsetAndRotation(2.0F, -8.4F, -1.0F, 0.0F, 0.0F, 0.25F));
        bow.addOrReplaceChild("knot", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.75F, -0.75F, 1.5F, 1.5F, 1.5F), PartPose.ZERO);
        bow.addOrReplaceChild("left", CubeListBuilder.create().texOffs(0, 0).addBox(-3.25F, -1.25F, -0.5F, 2.5F, 2.5F, 1.0F), PartPose.rotation(0, 0, 0.2F));
        bow.addOrReplaceChild("right", CubeListBuilder.create().texOffs(0, 0).addBox(0.75F, -1.25F, -0.5F, 2.5F, 2.5F, 1.0F), PartPose.rotation(0, 0, -0.2F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    private static final class BowLayer extends RenderLayer<Creeper, CreeperModel<Creeper>> {
        private static final ResourceLocation BOW_TEXTURE = new ResourceLocation(FemboyMod.MOD_ID, "textures/entity/cosmetic/fabric.png");
        private static final int BOW_COLOR = FastColor.ARGB32.opaque(0xFF4FA3);
        private final ModelPart bow;
        private final ModelPart head;
        private final RenderType type = RenderType.entityCutout(BOW_TEXTURE);

        BowLayer(RenderLayerParent<Creeper, CreeperModel<Creeper>> parent, ModelPart root) {
            super(parent);
            this.bow = root;
            this.head = parent.getModel().root().getChild("head");
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, Creeper creeper, float walkPos, float walkSpeed,
                           float partialTick, float ageInTicks, float headYaw, float headPitch) {
            if (creeper.isInvisible()) {
                return;
            }
            pose.pushPose();
            head.translateAndRotate(pose);
            bow.render(pose, buffers.getBuffer(type), light, OverlayTexture.NO_OVERLAY, BOW_COLOR);
            pose.popPose();
        }
    }
}
