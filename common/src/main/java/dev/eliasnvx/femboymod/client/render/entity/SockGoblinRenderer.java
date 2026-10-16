package dev.eliasnvx.femboymod.client.render.entity;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.entity.SockGoblin;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public final class SockGoblinRenderer extends HumanoidMobRenderer<SockGoblin, HumanoidRenderState, SockGoblinModel> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "sock_goblin"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/sock_goblin.png");
    private static final float SHADOW = 0.3F;

    public SockGoblinRenderer(EntityRendererProvider.Context context) {
        super(context, new SockGoblinModel(context.bakeLayer(LAYER)), SHADOW);
    }

    public static void registerLayers() {
        EntityModelLayerRegistry.register(LAYER, SockGoblinModel::createLayer);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
