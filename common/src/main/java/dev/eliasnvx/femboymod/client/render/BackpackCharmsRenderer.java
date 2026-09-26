package dev.eliasnvx.femboymod.client.render;

import dev.eliasnvx.femboymod.registry.FemboyItems;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderContext;
import dev.eliasnvx.femboymod.client.render.model.CosmeticModels;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.Map;

/** Charms dangling on the side of a worn backpack; static relative to the body, submitted as parts. */
final class BackpackCharmsRenderer {

    private static final int CHAIN_COLOR = 0xD9D2C0;
    private static final int DEFAULT_CHARM = 0xFFFFFF;
    private static final Map<String, Integer> CHARM_COLORS = Map.of(
            "shark_plush_charm", 0x6EB6E8, "cat_paw_charm", 0xF7B6CF, "heart_pin", 0xE8455A, "energy_can_charm", 0xF07AB0);

    private final ModelPart[] chains = new ModelPart[CosmeticModels.CHARM_SLOTS];
    private final ModelPart[] charms = new ModelPart[CosmeticModels.CHARM_SLOTS];
    private final ModelPart[][] flags = new ModelPart[CosmeticModels.CHARM_SLOTS][CosmeticModels.FLAG_STRIPES];
    private static final int BADGE_DEFAULT = 0xF291BE;
    private final RenderType type = RenderTypes.entityCutout(
            Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/cosmetic/fabric.png"));

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
        ItemContainerContents worn = ctx.stack().getOrDefault(FemboyComponents.CHARMS.get(), ItemContainerContents.EMPTY);
        if (worn.size() == 0) {
            return;
        }
        PoseStack pose = ctx.poseStack();
        pose.pushPose();
        ctx.parentModel().root().translateAndRotate(pose);
        ctx.parentModel().body.translateAndRotate(pose);
        int i = 0;
        for (ItemStackTemplate charm : worn.nonEmptyItems()) {
            if (i >= chains.length) {
                break;
            }
            ctx.collector().submitModelPart(chains[i], pose, type, ctx.light(), ctx.overlay(), null, ARGB.opaque(CHAIN_COLOR));
            if (charm.item().value() == FemboyItems.PRIDE_BADGE.get()) {
                // the badge's flag: stripes spread over the pattern (a 3-stripe flag shows each stripe ~twice as tall)
                Colorway colorway = charm.get(FemboyComponents.COLORWAY.get());
                int count = colorway == null ? 1 : colorway.stripeCount();
                for (int k = 0; k < CosmeticModels.FLAG_STRIPES; k++) {
                    int color = colorway == null ? BADGE_DEFAULT
                            : colorway.stripeColor(k * count / CosmeticModels.FLAG_STRIPES, ColorwayClock.ticks());
                    ctx.collector().submitModelPart(flags[i][k], pose, type, ctx.light(), ctx.overlay(), null, ARGB.opaque(color));
                }
            } else {
                int color = CHARM_COLORS.getOrDefault(BuiltInRegistries.ITEM.getKey(charm.item().value()).getPath(), DEFAULT_CHARM);
                ctx.collector().submitModelPart(charms[i], pose, type, ctx.light(), ctx.overlay(), null, ARGB.opaque(color));
            }
            i++;
        }
        pose.popPose();
    }
}
