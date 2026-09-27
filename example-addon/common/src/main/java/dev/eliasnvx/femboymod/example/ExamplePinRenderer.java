package dev.eliasnvx.femboymod.example;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.eliasnvx.femboymod.api.FemboyClientApi;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderContext;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;

/**
 * A custom cosmetic renderer: a small pin on the chest that follows the body. The item points to it
 * with {@code new Cosmetic(slot, Optional.of(ExampleAddon.PIN_RENDERER))}.
 */
public final class ExamplePinRenderer implements CosmeticRenderer {

    private static final ModelLayerLocation LAYER = new ModelLayerLocation(ExampleAddon.PIN_RENDERER, "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleAddon.MOD_ID,
            "textures/item/friendship_pin.png");
    private static final int TEXTURE_SIZE = 16;

    private final ModelPart pin;

    private ExamplePinRenderer(ModelPart pin) {
        this.pin = pin;
    }

    static void register(FemboyClientApi api) {
        EntityModelLayerRegistry.register(LAYER, ExamplePinRenderer::layer);
        api.cosmeticRenderers().register(ExampleAddon.PIN_RENDERER, models -> new ExamplePinRenderer(models.bakeLayer(LAYER)));
    }

    private static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        // Left side of the chest, just in front of the skin's jacket layer
        mesh.getRoot().addOrReplaceChild("pin", CubeListBuilder.create().texOffs(4, 4).addBox(-1.0F, -1.0F, -0.5F, 2, 2, 0.5F),
                PartPose.offset(2.0F, 3.5F, -2.3F));
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void submit(CosmeticRenderContext context) {
        PoseStack pose = context.poseStack();
        pose.pushPose();
        context.parentModel().body.translateAndRotate(pose);
        context.collector().submitModelPart(pin, pose, RenderTypes.entityCutout(TEXTURE), context.light(),
                context.overlay(), null, ARGB.opaque(0xFFFFFF));
        pose.popPose();
    }
}
