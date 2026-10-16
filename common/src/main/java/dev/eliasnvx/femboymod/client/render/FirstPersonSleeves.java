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
    /** Same defaults as the arm_warmers and nail_polish renderers. */
    private static final int WARMER_COLOR = 0xC8A2E8;
    private static final int NAIL_COLOR = 0xF291BE;
    /** Same defaults as the cat_paw_gloves renderer (white fur, pink pads). */
    private static final int PAW_COLOR = 0xFFF6F8;
    private static final int PAW_PAD_COLOR = 0xFF8FB8;
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
        var worn = CosmeticsManager.worn(player);
        ItemStack top = worn.get(FemboySlots.OUTFIT_TOP);
        if (top.is(FemboyTags.SLEEVED_TOPS) && !worn.isHidden(FemboySlots.OUTFIT_TOP)) {
            Colorway colorway = Colorways.effective(top).orElse(null);
            int main = colorway == null ? defaultTopColor(top) : colorway.stripeColor(0, ColorwayClock.ticks());
            part(root, side + "_sleeve", arm, pose, collector, light, main);
            part(root, side + "_cuff", arm, pose, collector, light, BuiltinCosmeticRenderers.shade(main, CUFF_SHADE));
        }
        ItemStack hands = worn.get(FemboySlots.HANDS);
        if (hands.is(FemboyItems.STRIPED_MITTENS.get()) && !worn.isHidden(FemboySlots.HANDS)) {
            Colorway colorway = Colorways.effective(hands).orElse(null);
            int main = colorway == null ? MITTEN_COLOR : colorway.stripeColor(0, ColorwayClock.ticks());
            int stripe = colorway == null ? MITTEN_STRIPE_COLOR : colorway.stripeColor(1, ColorwayClock.ticks());
            part(mittens, side + "_mitten", arm, pose, collector, light, main);
            part(mittens, side + "_mitten_stripes", arm, pose, collector, light, stripe);
            part(mittens, side + "_mitten_cuff", arm, pose, collector, light, BuiltinCosmeticRenderers.shade(main, CUFF_SHADE));
        }
        if (hands.is(FemboyItems.ARM_WARMERS.get()) && !worn.isHidden(FemboySlots.HANDS)) {
            Colorway colorway = Colorways.effective(hands).orElse(null);
            int main = colorway == null ? WARMER_COLOR : colorway.stripeColor(0, ColorwayClock.ticks());
            int stripe = colorway == null ? MITTEN_STRIPE_COLOR : colorway.stripeColor(1, ColorwayClock.ticks());
            part(mittens, side + "_warmer", arm, pose, collector, light, main);
            part(mittens, side + "_warmer_stripes", arm, pose, collector, light, stripe);
            part(mittens, side + "_warmer_cuff", arm, pose, collector, light, BuiltinCosmeticRenderers.shade(main, CUFF_SHADE));
        }
        if (hands.is(FemboyItems.CAT_PAW_GLOVES.get()) && !worn.isHidden(FemboySlots.HANDS)) {
            Colorway colorway = Colorways.effective(hands).orElse(null);
            int main = colorway == null ? PAW_COLOR : colorway.stripeColor(0, ColorwayClock.ticks());
            part(mittens, side + "_paw", arm, pose, collector, light, main);
            part(mittens, side + "_paw_cuff", arm, pose, collector, light, BuiltinCosmeticRenderers.shade(main, CUFF_SHADE));
            part(mittens, side + "_paw_pads", arm, pose, collector, light, PAW_PAD_COLOR);
        }
        if (hands.is(FemboyItems.NAIL_POLISH.get()) && !worn.isHidden(FemboySlots.HANDS)) {
            Colorway colorway = Colorways.effective(hands).orElse(null);
            part(mittens, side + "_nails", arm, pose, collector, light,
                    colorway == null ? NAIL_COLOR : colorway.stripeColor(0, ColorwayClock.ticks()));
        }
    }

    /** Undyed color of a hoodie-like top, as its renderer draws it. */
    private static int defaultTopColor(ItemStack top) {
        if (top.is(FemboyItems.PUMPKIN_HOODIE.get())) {
            return BuiltinCosmeticRenderers.PUMPKIN_ORANGE;
        }
        return top.is(FemboyItems.GHOST_SHEET.get()) ? BuiltinCosmeticRenderers.SHEET_WHITE : DEFAULT_COLOR;
    }

    private static void part(ModelPart parts, String name, ModelPart arm, PoseStack pose, SubmitNodeCollector collector, int light, int color) {
        ModelPart part = parts.getChild(name);
        part.loadPose(arm.storePose());
        collector.submitModelPart(part, pose, TYPE, light, OverlayTexture.NO_OVERLAY, null, ARGB.opaque(color));
    }
}
