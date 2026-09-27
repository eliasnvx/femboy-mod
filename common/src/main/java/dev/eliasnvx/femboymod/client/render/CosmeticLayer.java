package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.api.FemboyClientApi;
import dev.eliasnvx.femboymod.api.client.CosmeticMotion;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderContext;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderer;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Draws everything a player wears (SPEC §4.5). Added to every player renderer on both loaders. */
public final class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private final Map<ResourceLocation, CosmeticRenderer> renderers = new HashMap<>();
    private final Context context = new Context();
    /** Geo renderers per item id, created on first use (Blockbench models override code models). */
    private final Map<ResourceLocation, GeoCosmeticRenderer> geoRenderers = new HashMap<>();

    public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet models) {
        super(parent);
        ClientRegistries.renderers().ids().forEach(id -> {
            try {
                renderers.put(id, ClientRegistries.renderers().get(id).orElseThrow().create(models));
            } catch (RuntimeException e) {
                FemboyMod.LOGGER.error("Cosmetic renderer {} failed to initialize", id, e);
            }
        });
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        CosmeticRenderData data = ((CosmeticRenderStateAccess) state).femboymod$getCosmetics();
        if (data == null || state.isInvisible || !visibleFor(state)) {
            return;
        }
        List<CosmeticRenderData.Worn> worn = data.worn();
        context.set(poseStack, collector, light, LivingEntityRenderer.getOverlayCoords(state, 0.0F), state, getParentModel(), data);
        for (int i = 0; i < worn.size(); i++) {
            CosmeticRenderData.Worn item = worn.get(i);
            ResourceLocation itemId = item.itemId();
            CosmeticRenderer renderer = GeoCosmeticRenderer.hasModel(itemId)
                    ? geoRenderers.computeIfAbsent(itemId, GeoCosmeticRenderer::new)
                    : renderers.get(item.renderer());
            if (renderer != null) {
                context.worn = item;
                renderer.submit(context);
            }
        }
    }

    /** "Show other players' cosmetics" (client option): your own are always shown. */
    private static boolean visibleFor(AvatarRenderState state) {
        if (FemboyConfig.client().showOthersCosmetics()) {
            return true;
        }
        var self = net.minecraft.client.Minecraft.getInstance().player;
        return self != null && self.getId() == state.id;
    }

    /** Reused per submission; no allocation per frame. */
    private static final class Context implements CosmeticRenderContext {
        PoseStack poseStack;
        SubmitNodeCollector collector;
        int light;
        int overlay;
        AvatarRenderState state;
        PlayerModel parentModel;
        CosmeticMotion motion;
        CosmeticRenderData.Worn worn;

        void set(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, AvatarRenderState state,
                 PlayerModel parentModel, CosmeticMotion motion) {
            this.poseStack = poseStack;
            this.collector = collector;
            this.light = light;
            this.overlay = overlay;
            this.state = state;
            this.parentModel = parentModel;
            this.motion = motion;
        }

        @Override public PoseStack poseStack() { return poseStack; }
        @Override public SubmitNodeCollector collector() { return collector; }
        @Override public int light() { return light; }
        @Override public int overlay() { return overlay; }
        @Override public AvatarRenderState state() { return state; }
        @Override public PlayerModel parentModel() { return parentModel; }
        @Override public ItemStack stack() { return worn.stack(); }
        @Override public ResourceLocation slot() { return worn.slot(); }
        @Override public @Nullable Colorway colorway() { return worn.colorway(); }
        @Override public CosmeticMotion motion() { return motion; }
    }

    /** Static access to the client registries (the layer is built by vanilla, not by us). */
    public static final class ClientRegistries {
        private ClientRegistries() {
        }

        static dev.eliasnvx.femboymod.api.registry.ApiRegistry<CosmeticRenderer.Factory> renderers() {
            return FemboyClientApi.get().cosmeticRenderers();
        }
    }
}
