package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

/** Hoodie sleeve and mittens on the first-person hand (called from AvatarRendererHandMixin). */
public final class FirstPersonSleeves {

    private static final int DEFAULT_COLOR = 0xC8A2E8;
    private static final float CUFF_SHADE = -0.22F;
    /** Same defaults as the striped_mittens renderer (pink with white stripes). */
    private static final int MITTEN_COLOR = 0xF5A9B8;
    private static final int MITTEN_STRIPE_COLOR = 0xFFFFFF;
    private static final RenderType TYPE = RenderTypes.entityCutout(
            Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/cosmetic/knit.png"));

    private static EntityModelSet bakedFrom;
    private static ModelPart root;
    private static ModelPart mittens;

    private FirstPersonSleeves() {
    }

    public static void submit(PlayerModel model, ModelPart arm, PoseStack pose, SubmitNodeCollector collector, int light) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        if (models != bakedFrom) { // re-bake after resource reloads
            root = models.bakeLayer(CosmeticModels.HOODIE_FIRST_PERSON);
            mittens = models.bakeLayer(CosmeticModels.MITTENS_FIRST_PERSON);
            bakedFrom = models;
        }
        String side = arm == model.rightArm ? "right" : "left";
        var worn = CosmeticsManager.get(player);
        ItemStack top = worn.get(FemboySlots.OUTFIT_TOP);
        if (top.is(FemboyTags.HOODIES) && !worn.isHidden(FemboySlots.OUTFIT_TOP)) {
            Colorway colorway = Colorways.effective(top).orElse(null);
            int main = colorway == null ? DEFAULT_COLOR : colorway.stripeColor(0);
            part(root, side + "_sleeve", arm, pose, collector, light, main);
            part(root, side + "_cuff", arm, pose, collector, light, BuiltinCosmeticRenderers.shade(main, CUFF_SHADE));
        }
        ItemStack hands = worn.get(FemboySlots.HANDS);
        if (hands.is(FemboyItems.STRIPED_MITTENS.get()) && !worn.isHidden(FemboySlots.HANDS)) {
            Colorway colorway = Colorways.effective(hands).orElse(null);
            int main = colorway == null ? MITTEN_COLOR : colorway.stripeColor(0);
            int stripe = colorway == null ? MITTEN_STRIPE_COLOR : colorway.stripeColor(1);
            part(mittens, side + "_mitten", arm, pose, collector, light, main);
            part(mittens, side + "_mitten_stripes", arm, pose, collector, light, stripe);
            part(mittens, side + "_mitten_cuff", arm, pose, collector, light, BuiltinCosmeticRenderers.shade(main, CUFF_SHADE));
        }
    }

    private static void part(ModelPart parts, String name, ModelPart arm, PoseStack pose, SubmitNodeCollector collector, int light, int color) {
        ModelPart part = parts.getChild(name);
        part.loadPose(arm.storePose());
        collector.submitModelPart(part, pose, TYPE, light, OverlayTexture.NO_OVERLAY, null, ARGB.opaque(color));
    }
}
