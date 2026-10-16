package dev.eliasnvx.femboymod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Fluffy Rug: a thick shag carpet in any dye color. Right-click with an empty hand to change its shape (square, round,
 * heart), with a dye to recolor it. Like a carpet it needs something under it.
 */
public class FluffyRugBlock extends CarpetBlock {

    public enum Shape implements StringRepresentable {
        SQUARE("square"), ROUND("round"), HEART("heart");

        private final String name;

        Shape(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public Shape next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);
    private static final VoxelShape OUTLINE = Block.box(0.0, 0.0, 0.0, 16.0, 1.5, 16.0);
    private static final float SHAPE_PITCH = 1.2F;

    public FluffyRugBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SHAPE, Shape.SQUARE)
                .setValue(DyedFurniture.COLOR, DyedFurniture.DEFAULT_COLOR));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, DyedFurniture.COLOR);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        return DyedFurniture.dye(stack, state, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(SHAPE, state.getValue(SHAPE).next()), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, SHAPE_PITCH);
        }
        return InteractionResult.SUCCESS;
    }
}
