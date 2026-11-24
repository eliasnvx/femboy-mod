package dev.eliasnvx.femboymod.block;

import dev.eliasnvx.femboymod.registry.FemboySounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Pastel gamer desk with automatic monitor + keyboard combo snapping on top. */
public class GamerDeskBlock extends SimpleDecorBlock {

    public GamerDeskBlock(Properties properties) {
        super(properties, Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        BlockPos abovePos = pos.above();
        BlockState above = level.getBlockState(abovePos);
        if (above.is(FemboyBlocks.GAMER_MONITOR.get())
                && !above.getValue(GamerMonitorBlock.WITH_KEYBOARD)
                && stack.is(FemboyBlocks.GAMER_KEYBOARD.get().asItem())) {
            if (!level.isClientSide()) {
                level.setBlock(abovePos, above.setValue(GamerMonitorBlock.WITH_KEYBOARD, true), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, abovePos, FemboySounds.KEYBOARD_CLACK.get(), SoundSource.BLOCKS, 0.85F, 1.1F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (above.is(FemboyBlocks.GAMER_KEYBOARD.get())
                && stack.is(FemboyBlocks.GAMER_MONITOR.get().asItem())) {
            if (!level.isClientSide()) {
                BlockState combo = FemboyBlocks.GAMER_MONITOR.get().defaultBlockState()
                        .setValue(GamerMonitorBlock.FACING, above.getValue(GamerKeyboardBlock.FACING))
                        .setValue(GamerMonitorBlock.LIT, true)
                        .setValue(GamerMonitorBlock.WITH_KEYBOARD, true);
                level.setBlock(abovePos, combo, Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, abovePos, FemboySounds.KEYBOARD_CLACK.get(), SoundSource.BLOCKS, 0.85F, 1.15F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }
}
