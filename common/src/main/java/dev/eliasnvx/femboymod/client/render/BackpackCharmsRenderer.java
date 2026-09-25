package dev.eliasnvx.femboymod.client.render;

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
    private final RenderType type = RenderTypes.entityCutout(
            Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/cosmetic/fabric.png"));

    BackpackCharmsRenderer(ModelPart root) {
        for (int i = 0; i < CosmeticModels.CHARM_SLOTS; i++) {
            chains[i] = root.getChild("chain" + i);
            charms[i] = root.getChild("charm" + i);
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
            int color = CHARM_COLORS.getOrDefault(BuiltInRegistries.ITEM.getKey(charm.item().value()).getPath(), DEFAULT_CHARM);
            ctx.collector().submitModelPart(chains[i], pose, type, ctx.light(), ctx.overlay(), null, ARGB.opaque(CHAIN_COLOR));
            ctx.collector().submitModelPart(charms[i], pose, type, ctx.light(), ctx.overlay(), null, ARGB.opaque(color));
            i++;
        }
        pose.popPose();
    }
}
