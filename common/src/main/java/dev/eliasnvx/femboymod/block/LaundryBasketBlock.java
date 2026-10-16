package dev.eliasnvx.femboymod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Laundry Basket: a woven basket for clothes (9 slots). Sock Goblins go for it before they rob anyone. */
public class LaundryBasketBlock extends FurnitureStorageBlock {

    public static final MapCodec<LaundryBasketBlock> CODEC = simpleCodec(LaundryBasketBlock::new);

    public LaundryBasketBlock(Properties properties) {
        super(properties, Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0));
    }

    @Override
    protected MapCodec<LaundryBasketBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaundryBasketBlockEntity(pos, state);
    }
}
