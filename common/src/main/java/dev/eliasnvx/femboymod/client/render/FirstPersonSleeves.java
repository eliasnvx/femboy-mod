package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.client.render.model.CosmeticModels;
import dev.eliasnvx.femboymod.cosmetic.Colorways;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.ItemStack;

/** Hoodie sleeve and mittens on the first-person hand (called from PlayerRendererHandMixin). */
public final class FirstPersonSleeves {

    private static final int DEFAULT_COLOR = 0xC8A2E8;
    private static final float CUFF_SHADE = -0.22F;
    /** Same defaults as the striped_mittens renderer (pink with white stripes). */
    private static final int MITTEN_COLOR = 0xF5A9B8;
    private static final int MITTEN_STRIPE_COLOR = 0xFFFFFF;
    /** Same defaults as the arm_warmers and nail_polish renderers. */
    private static final int WARMER_COLOR = 0xC8A2E8;
    private static final int NAIL_COLOR = 0xF291BE;
    private static final RenderType TYPE = RenderType.entityCutout(
            ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/cosmetic/knit.png"));

    private static final String RIGHT = "right";
    private static final String LEFT = "left";

    private static EntityModelSet bakedFrom;
    private static ModelPart root;
    private static ModelPart mittens;

    /**
     * True while vanilla draws the first-person hand: its {@code setupAnim} call must not get emote poses
     * (the hand has its own fixed pose). Render thread only.
     */
    private static boolean renderingHand;

    private FirstPersonSleeves() {
    }

    public static boolean renderingHand() {
        return renderingHand;
    }

    public static void setRenderingHand(boolean value) {
        renderingHand = value;
    }

    public static void render(PlayerModel<?> model, ModelPart arm, PoseStack pose, MultiBufferSource buffers, int light,
                              AbstractClientPlayer player) {
        VertexConsumer buffer = buffers.getBuffer(TYPE);
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        if (models != bakedFrom) { // re-bake after resource reloads
            root = models.bakeLayer(CosmeticModels.HOODIE_FIRST_PERSON);
            mittens = models.bakeLayer(CosmeticModels.MITTENS_FIRST_PERSON);
            bakedFrom = models;
        }
        String side = arm == model.rightArm ? RIGHT : LEFT;
        var worn = CosmeticsManager.get(player);
        ItemStack top = worn.get(FemboySlots.OUTFIT_TOP);
        if (top.is(FemboyTags.HOODIES) && !worn.isHidden(FemboySlots.OUTFIT_TOP)) {
            Colorway colorway = Colorways.effective(top).orElse(null);
            int main = colorway == null ? DEFAULT_COLOR : colorway.stripeColor(0, ColorwayClock.ticks());
            part(root, side + "_sleeve", arm, pose, buffer, light, main);
            part(root, side + "_cuff", arm, pose, buffer, light, BuiltinCosmeticRenderers.shade(main, CUFF_SHADE));
        }
        ItemStack hands = worn.get(FemboySlots.HANDS);
        if (hands.is(FemboyItems.STRIPED_MITTENS.get()) && !worn.isHidden(FemboySlots.HANDS)) {
            Colorway colorway = Colorways.effective(hands).orElse(null);
            int main = colorway == null ? MITTEN_COLOR : colorway.stripeColor(0, ColorwayClock.ticks());
            int stripe = colorway == null ? MITTEN_STRIPE_COLOR : colorway.stripeColor(1, ColorwayClock.ticks());
            part(mittens, side + "_mitten", arm, pose, buffer, light, main);
            part(mittens, side + "_mitten_stripes", arm, pose, buffer, light, stripe);
            part(mittens, side + "_mitten_cuff", arm, pose, buffer, light, BuiltinCosmeticRenderers.shade(main, CUFF_SHADE));
        }
        if (hands.is(FemboyItems.ARM_WARMERS.get()) && !worn.isHidden(FemboySlots.HANDS)) {
            Colorway colorway = Colorways.effective(hands).orElse(null);
            int main = colorway == null ? WARMER_COLOR : colorway.stripeColor(0, ColorwayClock.ticks());
            int stripe = colorway == null ? MITTEN_STRIPE_COLOR : colorway.stripeColor(1, ColorwayClock.ticks());
            part(mittens, side + "_warmer", arm, pose, buffer, light, main);
            part(mittens, side + "_warmer_stripes", arm, pose, buffer, light, stripe);
            part(mittens, side + "_warmer_cuff", arm, pose, buffer, light, BuiltinCosmeticRenderers.shade(main, CUFF_SHADE));
        }
        if (hands.is(FemboyItems.NAIL_POLISH.get()) && !worn.isHidden(FemboySlots.HANDS)) {
            Colorway colorway = Colorways.effective(hands).orElse(null);
            part(mittens, side + "_nails", arm, pose, buffer, light,
                    colorway == null ? NAIL_COLOR : colorway.stripeColor(0, ColorwayClock.ticks()));
        }
    }

    private static void part(ModelPart parts, String name, ModelPart arm, PoseStack pose, VertexConsumer buffer, int light, int color) {
        ModelPart part = parts.getChild(name);
        part.copyFrom(arm);
        part.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, ARGB32.opaque(color));
    }
}
