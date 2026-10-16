package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.block.FemboyBlocks;
import dev.eliasnvx.femboymod.block.MirrorBlock;
import dev.eliasnvx.femboymod.block.MirrorBlockEntity;
import dev.eliasnvx.femboymod.menu.CosmeticsMenu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** The two-block mirror: halves stay together, one block entity, right-click opens the Outfit screen. */
public final class MirrorGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("mirror_halves_break_together", MirrorGameTests::mirrorHalvesBreakTogether),
            new CosmeticGameTests.Entry("mirror_opens_the_outfit_screen", MirrorGameTests::mirrorOpensTheOutfitScreen));

    private static final BlockPos LOWER = new BlockPos(1, 1, 1);

    private MirrorGameTests() {
    }

    private static void place(GameTestHelper helper) {
        BlockState lower = FemboyBlocks.MIRROR.get().defaultBlockState().setValue(MirrorBlock.FACING, Direction.NORTH);
        helper.setBlock(LOWER, lower);
        helper.setBlock(LOWER.above(), lower.setValue(MirrorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    public static void mirrorHalvesBreakTogether(GameTestHelper helper) {
        place(helper);
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(LOWER)) instanceof MirrorBlockEntity,
                "lower half has the block entity");
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(LOWER.above())) == null, "upper half has none");
        helper.setBlock(LOWER, Blocks.AIR);
        helper.assertBlockPresent(Blocks.AIR, LOWER.above());
        place(helper);
        helper.setBlock(LOWER.above(), Blocks.AIR);
        helper.assertBlockPresent(Blocks.AIR, LOWER);
        helper.succeed();
    }

    public static void mirrorOpensTheOutfitScreen(GameTestHelper helper) {
        place(helper);
        WearableGameTests.withPlayer(helper, player -> {
            helper.useBlock(LOWER.above(), player);
            helper.assertTrue(player.containerMenu instanceof CosmeticsMenu, "Outfit screen open, got " + player.containerMenu);
            player.closeContainer();
        });
    }
}
