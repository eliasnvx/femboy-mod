package dev.eliasnvx.femboymod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** A dye recolors the wax; otherwise (main hand, like 1.21.1's {@code useWithoutItem}) it switches on or off. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult dyed = DyedFurniture.dye(player.getItemInHand(hand), state, level, pos, player);
        if (dyed != InteractionResult.PASS || hand != InteractionHand.MAIN_HAND) {
            return dyed;
        }
        if (!level.isClientSide()) {
            boolean lit = !state.getValue(LIT);
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, /* 1.20.1 has no copper bulb sound */ SoundSource.BLOCKS,
                    0.4F, lit ? 1.1F : 0.8F);
        }
        return InteractionResult.SUCCESS;
    }
}
