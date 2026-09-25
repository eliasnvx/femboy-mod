package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.client.render.model.CosmeticModels;
import dev.eliasnvx.femboymod.cosmetic.Colorways;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyItems;
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

/** Hoodie sleeve on the first-person hand (called from AvatarRendererHandMixin). */
public final class FirstPersonSleeves {

    private static final int DEFAULT_COLOR = 0xC8A2E8;
    private static final float CUFF_SHADE = -0.22F;
    private static final RenderType TYPE = RenderTypes.entityCutout(
            Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/cosmetic/knit.png"));

    private static EntityModelSet bakedFrom;
    private static ModelPart root;

    private FirstPersonSleeves() {
    }

    public static void submit(PlayerModel model, ModelPart arm, PoseStack pose, SubmitNodeCollector collector, int light) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack top = CosmeticsManager.get(player).get(FemboySlots.OUTFIT_TOP);
        if (!top.is(FemboyItems.OVERSIZED_HOODIE.get())) {
            return;
        }
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        if (models != bakedFrom) { // re-bake after resource reloads
            root = models.bakeLayer(CosmeticModels.HOODIE_FIRST_PERSON);
            bakedFrom = models;
        }
        String side = arm == model.rightArm ? "right" : "left";
        ModelPart sleeve = root.getChild(side + "_sleeve");
        ModelPart cuff = root.getChild(side + "_cuff");
        sleeve.loadPose(arm.storePose());
        cuff.loadPose(arm.storePose());

        Colorway colorway = Colorways.effective(top).orElse(null);
        int main = colorway == null ? DEFAULT_COLOR : colorway.stripeColor(0);
        collector.submitModelPart(sleeve, pose, TYPE, light, OverlayTexture.NO_OVERLAY, null, ARGB.opaque(main));
        collector.submitModelPart(cuff, pose, TYPE, light, OverlayTexture.NO_OVERLAY, null,
                ARGB.opaque(BuiltinCosmeticRenderers.shade(main, CUFF_SHADE)));
    }
}
