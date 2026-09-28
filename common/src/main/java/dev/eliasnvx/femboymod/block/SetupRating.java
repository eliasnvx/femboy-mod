package dev.eliasnvx.femboymod.block;

import dev.eliasnvx.femboymod.FemboyMod;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/**
 * Gamer corner "setup rating": one star per kind of setup piece near a Gamer Chair. Which blocks count is
 * data-driven (block tags {@code #femboymod:setup/<kind>}); posters are painting variants in
 * {@code #femboymod:posters}. Lit-able blocks (the monitor) only count while on.
 */
public final class SetupRating {

    /** One kind of setup piece; each kind found adds one star. */
    public record Kind(String name, TagKey<Block> blocks) {
    }

    public static final List<Kind> BLOCK_KINDS = List.of(
            kind("screen"), kind("input"), kind("lighting"), kind("comfort"), kind("debugging"));
    public static final String POSTER_KIND = "poster";
    public static final TagKey<PaintingVariant> POSTERS =
            TagKey.create(Registries.PAINTING_VARIANT, new ResourceLocation(FemboyMod.MOD_ID, "posters"));
    /** Block kinds plus posters. */
    public static final int MAX = BLOCK_KINDS.size() + 1;

    private static final String STAR = "★";
    private static final String EMPTY_STAR = "☆";

    private SetupRating() {
    }

    private static Kind kind(String name) {
        return new Kind(name, TagKey.create(Registries.BLOCK, new ResourceLocation(FemboyMod.MOD_ID, "setup/" + name)));
    }

    /** Number of distinct setup kinds within {@code radius} blocks of {@code center} (0..{@link #MAX}). */
    public static int evaluate(ServerLevel level, BlockPos center, int radius) {
        int found = 0;
        for (Kind kind : BLOCK_KINDS) {
            if (hasBlock(level, center, radius, kind.blocks())) {
                found++;
            }
        }
        AABB area = new AABB(center).inflate(radius);
        if (!level.getEntitiesOfClass(Painting.class, area, painting -> painting.getVariant().is(POSTERS)).isEmpty()) {
            found++;
        }
        return found;
    }

    private static boolean hasBlock(ServerLevel level, BlockPos center, int radius, TagKey<Block> tag) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(tag) && (!state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT))) {
                return true;
            }
        }
        return false;
    }

    /** "Setup: ★★★☆☆" for the action bar. */
    public static Component message(int rating) {
        MutableComponent stars = Component.literal(STAR.repeat(rating) + EMPTY_STAR.repeat(Math.max(0, MAX - rating)));
        return Component.translatable("message.femboymod.setup_rating", stars);
    }
}
