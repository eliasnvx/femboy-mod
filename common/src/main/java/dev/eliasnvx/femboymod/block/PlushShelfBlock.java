package dev.eliasnvx.femboymod.block;

import dev.eliasnvx.femboymod.item.PlushBlockItem;
import dev.eliasnvx.femboymod.registry.FemboySounds;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Wall-mounted plushie shelf: holds up to 3 plushies, cans or trinkets.
 * Right-click with an item to place it; right-click empty-handed to squish any plushie on it (sneak to take).
 */
public class PlushShelfBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape NORTH_SHAPE = Block.box(0.0, 5.0, 10.0, 16.0, 10.0, 16.0);
    private final Map<Direction, VoxelShape> shapes = Shapes.rotateHorizontal(NORTH_SHAPE);

    public PlushShelfBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlushShelfBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof PlushShelfBlockEntity shelf)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            if (!shelf.place(player.getAbilities().instabuild ? stack.copy() : stack)) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (stack.getItem() instanceof PlushBlockItem) {
                level.blockEvent(pos, this, PlushBlockEntity.EVENT_PLACE, 0);
                level.playSound(null, pos, FemboySounds.PLUSH_SQUEAK.get(), SoundSource.BLOCKS, 0.35F,
                        1.05F + level.getRandom().nextFloat() * 0.2F);
            } else {
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.7F, 1.2F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PlushShelfBlockEntity shelf)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            if (!player.isShiftKeyDown() && shelf.hasPlush()) {
                level.blockEvent(pos, this, PlushBlockEntity.EVENT_CLICK, 0);
                float pitch = 1.3F + level.getRandom().nextFloat() * 0.4F;
                level.playSound(null, pos, FemboySounds.PLUSH_SQUEAK.get(), SoundSource.BLOCKS, 0.9F, pitch);
                serverLevel.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 1, 0.15, 0.1, 0.15, 0.0);
                return InteractionResult.SUCCESS;
            }
            ItemStack taken = shelf.takeLast();
            if (taken.isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
            level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.5F, 1.3F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
}
