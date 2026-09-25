package dev.eliasnvx.femboymod.client.render.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;

import java.util.EnumMap;
import java.util.Map;

/**
 * Color groups of a cosmetic model. Cubes are collected per group under holder parts named
 * {@code g_<group>}; a model instance shows one group and is submitted with that group's tint,
 * so one model gets several colors without per-model textures.
 */
public enum Groups {
    /** Tinted with the colorway (first stripe). */
    MAIN,
    /** Tinted with a lighter/darker shade of the main color (inner ear, cuffs, pocket). */
    ACCENT,
    /** Fixed light color (fur tufts, drawstrings, tail tip). */
    DETAIL,
    /** Fixed dark color (bell slit, aglets, clip bar). */
    DARK,
    /** Fixed metallic color (bell, ring). */
    METAL;

    public String holderName() {
        return "g_" + name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Shows only {@code visible}'s holders in every part of the model tree. */
    public static void show(ModelPart root, Groups visible) {
        for (ModelPart part : root.getAllParts()) {
            for (Groups group : values()) {
                if (part.hasChild(group.holderName())) {
                    part.getChild(group.holderName()).visible = group == visible;
                }
            }
        }
    }

    /** Factory for models without custom animation. */
    public interface GroupModelFactory {
        java.util.function.BiFunction<ModelPart, Groups, CosmeticModels.GroupModel> PLAIN = CosmeticModels.GroupModel::new;
    }

    /** Collects boxes per group for one pivot part, then attaches them as holder children. */
    public static final class Builder {
        private final PartDefinition pivot;
        private final Map<Groups, CubeListBuilder> cubes = new EnumMap<>(Groups.class);
        private int texCursor;

        public Builder(PartDefinition pivot) {
            this.pivot = pivot;
        }

        /** Adds a box; texture offsets are varied per box so tiny voxels pick up texture variation. */
        public Builder box(Groups group, float x, float y, float z, float w, float h, float d, CubeDeformation inflate) {
            int u = (texCursor * 7) % 48;
            int v = (texCursor * 13) % 48;
            texCursor++;
            cubes.computeIfAbsent(group, g -> CubeListBuilder.create()).texOffs(u, v).addBox(x, y, z, w, h, d, inflate);
            return this;
        }

        public Builder box(Groups group, float x, float y, float z, float w, float h, float d) {
            return box(group, x, y, z, w, h, d, CubeDeformation.NONE);
        }

        /**
         * Extrudes a pixel mask into voxels (rows top to bottom, runs of the same char merged).
         * {@code legend} maps mask chars to groups; other chars are empty.
         *
         * @param x0    model x of the mask's left edge
         * @param y0    model y of the mask's top edge
         * @param z0    model z of the front face
         * @param pixel size of one mask pixel
         * @param depth thickness
         */
        public Builder extrude(String[] rows, Map<Character, Groups> legend, float x0, float y0, float z0, float pixel, float depth) {
            for (int row = 0; row < rows.length; row++) {
                String line = rows[row];
                int col = 0;
                while (col < line.length()) {
                    char c = line.charAt(col);
                    Groups group = legend.get(c);
                    int end = col + 1;
                    while (end < line.length() && line.charAt(end) == c) {
                        end++;
                    }
                    if (group != null) {
                        box(group, x0 + col * pixel, y0 + row * pixel, z0, (end - col) * pixel, pixel, depth);
                    }
                    col = end;
                }
            }
            return this;
        }

        /** Attaches the holders; call once after adding all boxes. */
        public PartDefinition build() {
            cubes.forEach((group, list) -> pivot.addOrReplaceChild(group.holderName(), list, PartPose.ZERO));
            return pivot;
        }
    }
}
