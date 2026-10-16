package dev.eliasnvx.femboymod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lava Lamp: wax blobs rise and sink slowly in a glowing glass (a smooth, slow texture animation; nothing blinks).
 * Right-click turns it on and off, a dye recolors the wax. Counts as setup lighting while on.
 */
public class LavaLampBlock extends Block {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int LIGHT = 10;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(5.0, 0.0, 5.0, 11.0, 3.0, 11.0),
            Block.box(5.5, 3.0, 5.5, 10.5, 12.0, 10.5),
            Block.box(6.5, 12.0, 6.5, 9.5, 14.0, 9.5));

    public LavaLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, true).setValue(DyedFurniture.COLOR, DyedFurniture.DEFAULT_COLOR));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, DyedFurniture.COLOR);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        return DyedFurniture.dye(stack, state, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            boolean lit = !state.getValue(LIT);
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.4F, lit ? 1.1F : 0.8F);
        }
        return InteractionResult.SUCCESS;
    }
}
