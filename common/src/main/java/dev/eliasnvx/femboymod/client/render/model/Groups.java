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
    METAL,
    /**
     * Main-colored geometry split into {@link #BANDS} horizontal bands, so patterns can color each band
     * (pride stripes). Not a render group itself: its cubes go to holders {@code g_band<i>}.
     */
    BANDED;

    /** Number of horizontal pattern bands per banded range. */
    public static final int BANDS = 12;
    /** Band argument for {@link #show}: main parts plus all bands (solid color). */
    public static final int ALL_BANDS = -1;
    /** Band argument for {@link #show}: only the non-banded main parts. */
    public static final int NO_BANDS = -2;

    public static String bandHolderName(int band) {
        return "g_band" + band;
    }

    public String holderName() {
        return "g_" + name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Shows only {@code visible}'s holders in every part of the model tree. For {@link #MAIN},
     * {@code band} selects banded geometry: {@link #ALL_BANDS}, {@link #NO_BANDS} or one band index.
     */
    public static void show(ModelPart root, Groups visible, int band) {
        for (ModelPart part : root.getAllParts()) {
            for (Groups group : values()) {
                if (group != BANDED && part.hasChild(group.holderName())) {
                    part.getChild(group.holderName()).visible = group == visible && (group != MAIN || band < 0);
                }
            }
            for (int i = 0; i < BANDS; i++) {
                if (part.hasChild(bandHolderName(i))) {
                    part.getChild(bandHolderName(i)).visible = visible == MAIN && (band == ALL_BANDS || band == i);
                }
            }
        }
    }

    /** Factory for models without custom animation. */
    public interface GroupModelFactory {
        Factory PLAIN = CosmeticModels.GroupModel::new;

        /** Creates a model instance showing one group (and band selection for MAIN). */
        @FunctionalInterface
        interface Factory {
            CosmeticModels.GroupModel create(ModelPart root, Groups group, int band);
        }
    }

    /** Collects boxes per group for one pivot part, then attaches them as holder children. */
    public static final class Builder {
        private final PartDefinition pivot;
        private final Map<Groups, CubeListBuilder> cubes = new EnumMap<>(Groups.class);
        private final CubeListBuilder[] bands = new CubeListBuilder[BANDS];
        private int texCursor;
        private float bandTop = 0.0F;
        private float bandBottom = 12.0F;

        /** Vertical range (pivot-local y, top < bottom) that the {@link #BANDS} bands divide. */
        public Builder bandRange(float top, float bottom) {
            this.bandTop = top;
            this.bandBottom = bottom;
            return this;
        }

        private int bandAt(float y) {
            int band = (int) ((y - bandTop) / (bandBottom - bandTop) * BANDS);
            return Math.max(0, Math.min(BANDS - 1, band));
        }

        private void addToBand(int band, float x, float y, float z, float w, float h, float d) {
            int u = (texCursor * 7) % 48;
            int v = (texCursor * 13) % 48;
            texCursor++;
            if (bands[band] == null) {
                bands[band] = CubeListBuilder.create();
            }
            bands[band].texOffs(u, v).addBox(x, y, z, w, h, d);
        }

        /**
         * Adds a main-colored box split into pattern bands. The inflation is applied to the outer shell
         * only, so neighbouring bands never overlap (no z-fighting at seams).
         */
        public Builder bandedBox(float x, float y, float z, float w, float h, float d, float inflate) {
            float x0 = x - inflate, z0 = z - inflate, y0 = y - inflate, y1 = y + h + inflate;
            float wx = w + 2 * inflate, wz = d + 2 * inflate;
            float step = (bandBottom - bandTop) / BANDS;
            float cursor = y0;
            while (cursor < y1 - 1.0E-4F) {
                int band = bandAt(cursor + 1.0E-4F);
                float bandEnd = band == BANDS - 1 ? Float.MAX_VALUE : bandTop + (band + 1) * step;
                float next = Math.min(y1, Math.max(bandEnd, cursor + 1.0E-3F));
                addToBand(band, x0, cursor, z0, wx, next - cursor, wz);
                cursor = next;
            }
            return this;
        }

        /** Adds a whole box to the band containing {@code bandY} (e.g. one tail segment = one ring). */
        public Builder boxInBand(float bandY, float x, float y, float z, float w, float h, float d) {
            addToBand(bandAt(bandY), x, y, z, w, h, d);
            return this;
        }

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
                    if (group == BANDED) {
                        float y = y0 + row * pixel;
                        addToBand(bandAt(y + pixel / 2), x0 + col * pixel, y, z0, (end - col) * pixel, pixel, depth);
                    } else if (group != null) {
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
            for (int i = 0; i < BANDS; i++) {
                if (bands[i] != null) {
                    pivot.addOrReplaceChild(bandHolderName(i), bands[i], PartPose.ZERO);
                }
            }
            return pivot;
        }
    }
}
