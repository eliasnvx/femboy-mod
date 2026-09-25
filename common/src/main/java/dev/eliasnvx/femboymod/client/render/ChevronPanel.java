package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderContext;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.client.render.model.CosmeticModels;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * A grid of thin cells on a flat, body-attached surface (the hoodie chest) used to draw 2D pattern
 * parts such as the Progress chevron. Cells are static relative to the body, so they are submitted as
 * model parts under the body transform captured now (safe with deferred rendering). Only cells inside the
 * chevron are drawn; stripes underneath come from the banded model.
 */
public final class ChevronPanel {

    private final ModelPart[] cells;
    private final RenderType type;

    public ChevronPanel(ModelPart root, Identifier texture) {
        this.cells = new ModelPart[CosmeticModels.PANEL_COLUMNS * CosmeticModels.PANEL_ROWS];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = root.getChild("cell" + i);
        }
        this.type = RenderTypes.entityCutout(texture);
    }

    void submit(CosmeticRenderContext ctx, Colorway colorway) {
        PoseStack pose = ctx.poseStack();
        pose.pushPose();
        ctx.parentModel().root().translateAndRotate(pose);
        ctx.parentModel().body.translateAndRotate(pose);
        for (int row = 0; row < CosmeticModels.PANEL_ROWS; row++) {
            float v = (row + 0.5F) / CosmeticModels.PANEL_ROWS;
            for (int col = 0; col < CosmeticModels.PANEL_COLUMNS; col++) {
                // viewer's left = the wearer's right side (-x) = the flag's hoist
                float u = (col + 0.5F) / CosmeticModels.PANEL_COLUMNS;
                if (colorway.pattern().orElseThrow().value().chevron().orElseThrow().bandAt(u, v) < 0) {
                    continue;
                }
                ctx.collector().submitModelPart(cells[row * CosmeticModels.PANEL_COLUMNS + col], pose, type,
                        ctx.light(), ctx.overlay(), null, ARGB.opaque(colorway.colorAt(u, v)));
            }
        }
        pose.popPose();
    }
}
