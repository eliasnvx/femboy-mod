package dev.eliasnvx.femboymod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Mini Fridge: keeps 9 drinks and snacks and chills Byte Energy ({@link MiniFridgeBlockEntity}). */
public class MiniFridgeBlock extends FurnitureStorageBlock {

    public MiniFridgeBlock(Properties properties) {
        super(properties, Block.box(1.0, 0.0, 2.0, 15.0, 15.0, 15.0));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MiniFridgeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, FemboyBlocks.MINI_FRIDGE_ENTITY.get(), MiniFridgeBlockEntity::serverTick);
    }
}
