package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderContext;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.client.render.model.CosmeticModels;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import dev.eliasnvx.femboymod.item.ItemList;

import java.util.Map;

/** Charms dangling on the side of a worn backpack; static relative to the body, drawn as parts. */
final class BackpackCharmsRenderer {

    private static final int CHAIN_COLOR = 0xD9D2C0;
    private static final int DEFAULT_CHARM = 0xFFFFFF;
    private static final Map<String, Integer> CHARM_COLORS = Map.of(
            "shark_plush_charm", 0x6EB6E8, "cat_paw_charm", 0xF7B6CF, "heart_pin", 0xE8455A, "energy_can_charm", 0xF07AB0);

    private final ModelPart[] chains = new ModelPart[CosmeticModels.CHARM_SLOTS];
    private final ModelPart[] charms = new ModelPart[CosmeticModels.CHARM_SLOTS];
    private final ModelPart[][] flags = new ModelPart[CosmeticModels.CHARM_SLOTS][CosmeticModels.FLAG_STRIPES];
    private static final int BADGE_DEFAULT = 0xF291BE;
    private final RenderType type = RenderType.entityCutout(
            new ResourceLocation(FemboyMod.MOD_ID, "textures/entity/cosmetic/fabric.png"));

    BackpackCharmsRenderer(ModelPart root) {
        for (int i = 0; i < CosmeticModels.CHARM_SLOTS; i++) {
            chains[i] = root.getChild("chain" + i);
            charms[i] = root.getChild("charm" + i);
            for (int k = 0; k < CosmeticModels.FLAG_STRIPES; k++) {
                flags[i][k] = root.getChild("flag" + i + "_" + k);
            }
        }
    }

    void submit(CosmeticRenderContext ctx) {
        ItemList worn = FemboyComponents.CHARMS.getOrDefault(ctx.stack(), ItemList.EMPTY);
        if (worn.size() == 0) {
            return;
        }
        PoseStack pose = ctx.poseStack();
        VertexConsumer buffer = ctx.bufferSource().getBuffer(type);
        int light = ctx.light();
        int overlay = ctx.overlay();
        pose.pushPose();
        ctx.parentModel().body.translateAndRotate(pose);
        int i = 0;
        for (int slot = 0; slot < worn.size() && i < chains.length; slot++) {
            ItemStack charm = worn.peek(slot); // read-only, no copy per frame
            if (charm.isEmpty()) {
                continue;
            }
            ModelColors.render(chains[i], pose, buffer, light, overlay, ModelColors.opaque(CHAIN_COLOR));
            if (charm.is(FemboyItems.PRIDE_BADGE.get())) {
                // the badge's flag: stripes spread over the pattern (a 3-stripe flag shows each stripe ~twice as tall)
                Colorway colorway = FemboyComponents.COLORWAY.get(charm);
                int count = colorway == null ? 1 : colorway.stripeCount();
                for (int k = 0; k < CosmeticModels.FLAG_STRIPES; k++) {
                    int color = colorway == null ? BADGE_DEFAULT
                            : colorway.stripeColor(k * count / CosmeticModels.FLAG_STRIPES, ColorwayClock.ticks());
                    ModelColors.render(flags[i][k], pose, buffer, light, overlay, ModelColors.opaque(color));
                }
            } else {
                int color = CHARM_COLORS.getOrDefault(BuiltInRegistries.ITEM.getKey(charm.getItem()).getPath(), DEFAULT_CHARM);
                ModelColors.render(charms[i], pose, buffer, light, overlay, ModelColors.opaque(color));
            }
            i++;
        }
        pose.popPose();
    }
}
