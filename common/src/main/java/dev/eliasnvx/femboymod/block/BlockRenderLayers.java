package dev.eliasnvx.femboymod.block;

import java.util.List;

/**
 * Which of our blocks draw with alpha cutout. 26.3 picks a block's render layer from its textures; 1.20.1 draws every
 * block solid unless told otherwise, and transparent texture pixels then come out black (ore overlays, crystal buds,
 * plants, the neon signs' glowing plane). The rule: a block is listed when a face of its models samples a texture pixel
 * that isn't fully opaque ({@code BlockRenderLayersTest} checks both lists against the block models and textures).
 * Plain ids in our namespace, so the check needs no game classes; the client registers them once at startup
 * ({@code FemboyModClient}), nothing per frame.
 */
public final class BlockRenderLayers {

    /** Cross models and thin planes, like vanilla's plants and amethyst buds ({@code RenderType.cutout()}). */
    public static final List<String> CUTOUT = List.of(
            "debug_flytrap", "potted_debug_flytrap", "catnip", "potted_catnip", "potted_grass",
            "small_rose_quartz_bud", "medium_rose_quartz_bud", "large_rose_quartz_bud", "rose_quartz_cluster",
            "neon_sign");

    /**
     * Full blocks with a transparent overlay over a vanilla base, the ores: mipmapped like vanilla's grass block, which
     * is built the same way ({@code RenderType.cutoutMipped()}).
     */
    public static final List<String> CUTOUT_MIPPED = List.of(
            "rose_quartz_ore", "deepslate_rose_quartz_ore", "deepslate_moonstone_ore", "glitter_ore", "neon_quartz_ore");

    private BlockRenderLayers() {
    }
}
