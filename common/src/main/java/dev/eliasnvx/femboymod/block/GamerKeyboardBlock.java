package dev.eliasnvx.femboymod.block;

import com.mojang.serialization.MapCodec;
import dev.eliasnvx.femboymod.registry.FemboySounds;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Mechanical keyboard with a mouse: right-click to clack the keys. */
public class GamerKeyboardBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<GamerKeyboardBlock> CODEC = simpleCodec(GamerKeyboardBlock::new);

    private static final float MIN_PITCH = 1.5F;
    private static final float PITCH_RANGE = 0.5F;
    private static final VoxelShape NORTH_SHAPE = Block.box(1.0, 0.0, 5.0, 15.0, 2.0, 11.0);
    private final Map<Direction, VoxelShape> shapes = BlockShapes.rotateHorizontal(NORTH_SHAPE);

    public GamerKeyboardBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<GamerKeyboardBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                                  Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (stack.is(FemboyBlocks.GAMER_MONITOR.get().asItem())) {
            if (!level.isClientSide()) {
                BlockState combo = FemboyBlocks.GAMER_MONITOR.get().defaultBlockState()
                        .setValue(GamerMonitorBlock.FACING, state.getValue(FACING))
                        .setValue(GamerMonitorBlock.LIT, true)
                        .setValue(GamerMonitorBlock.WITH_KEYBOARD, true);
                level.setBlock(pos, combo, Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, FemboySounds.KEYBOARD_CLACK.get(), SoundSource.BLOCKS, 0.85F, 1.15F);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            float pitch = 0.92F + level.getRandom().nextFloat() * 0.16F;
            level.playSound(null, pos, FemboySounds.KEYBOARD_CLACK.get(), SoundSource.BLOCKS, 0.85F, pitch);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 2, 0.2, 0.05, 0.2, 0.01);
        }
        return InteractionResult.SUCCESS;
    }
}
