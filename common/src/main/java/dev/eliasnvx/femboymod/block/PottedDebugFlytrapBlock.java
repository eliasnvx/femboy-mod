package dev.eliasnvx.femboymod.block;

import dev.eliasnvx.femboymod.config.FemboyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * A Debug Flytrap in a flower pot: a desk bug catcher. It hunts like the planted one with the first stage's reach,
 * but a pot keeps it small, so it never grows (taking it out gives a fresh flytrap). No block entity: cats can push
 * it off a table like any pot.
 */
public class PottedDebugFlytrapBlock extends FlowerPotBlock {

    public PottedDebugFlytrapBlock(Block plant, Properties properties) {
        super(plant, properties);
        registerDefaultState(stateDefinition.any().setValue(DebugFlytrapBlock.DIGESTING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DebugFlytrapBlock.DIGESTING);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this) && level instanceof ServerLevel server) {
            DebugFlytrapBlock.keepTicking(server, pos, state, this); // placed digesting (commands, structures): digests in full
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        DebugFlytrapBlock.keepTicking(level, pos, state, this);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(DebugFlytrapBlock.DIGESTING)) {
            DebugFlytrapBlock.finishDigesting(level, pos, state);
        } else if (!DebugFlytrapBlock.snap(level, pos, state, DebugFlytrapBlock.reach(0, FemboyConfig.common().plants()))) {
            level.scheduleTick(pos, this, FemboyConfig.common().plants().flytrapCheckInterval());
        }
    }
}
