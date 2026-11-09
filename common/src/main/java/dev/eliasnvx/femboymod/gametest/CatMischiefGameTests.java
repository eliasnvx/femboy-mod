package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.entity.CatKnockGoal;
import dev.eliasnvx.femboymod.entity.CatMischief;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import dev.eliasnvx.femboymod.world.FemboyGameRules;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

/** Cats knocking things off tables (0.3.0): the edge rule, no item lost or duplicated, the game rules, a real cat. */
public final class CatMischiefGameTests {

    /** A real cat walks over, jumps on the table and pushes; generous for the walk (also the Fabric glue's timeout). */
    public static final int REAL_CAT_TICKS = 300;

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("knocked_pot_comes_off_as_its_drops", CatMischiefGameTests::knockedPotComesOffAsItsDrops),
            new CosmeticGameTests.Entry("cat_knockable_has_no_block_entities", CatMischiefGameTests::catKnockableHasNoBlockEntities),
            new CosmeticGameTests.Entry("cat_mischief_needs_the_game_rules", CatMischiefGameTests::catMischiefNeedsTheGameRules),
            new CosmeticGameTests.Entry("cat_knocks_a_pot_off_the_table", CatMischiefGameTests::catKnocksAPotOffTheTable, REAL_CAT_TICKS));

    /** A quick, certain cat: checks every second, always goes, barely stares. */
    private static final CommonConfig.Cats EAGER = new CommonConfig.Cats(20, 1.0F, 6000, 6, 12.0, 5);
    /** Floor at y 1, a two-block table at y 2, the pot on its east end: only east has a drop and room to stand. */
    static final BlockPos POT = new BlockPos(3, 3, 2);
    static final BlockPos CAT = new BlockPos(1, 2, 2);
    static final BlockPos WATCHER = new BlockPos(1, 2, 0);
    private static final int FLOOR_X = 7;
    private static final int FLOOR_Z = 5;

    private CatMischiefGameTests() {
    }

    static void buildTable(GameTestHelper helper) {
        for (int x = 0; x < FLOOR_X; x++) {
            for (int z = 0; z < FLOOR_Z; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        helper.setBlock(POT.below().west(), Blocks.OAK_PLANKS);
        helper.setBlock(POT.below(), Blocks.OAK_PLANKS);
        helper.setBlock(POT, Blocks.POTTED_POPPY);
    }

    private static void assertPotAndPoppyOnce(GameTestHelper helper) {
        helper.assertBlockPresent(Blocks.AIR, POT);
        List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds());
        helper.assertValueEqual(count(items, Items.FLOWER_POT), 1, "flower pots");
        helper.assertValueEqual(count(items, Items.POPPY), 1, "poppies");
        helper.assertValueEqual(items.size(), 2, "nothing else dropped");
    }

    private static int count(List<ItemEntity> items, Item item) {
        return items.stream().filter(entity -> entity.getItem().is(item)).mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    /** The pot is found at the table's east edge, and pushing it gives exactly its drops: the pot and the poppy. */
    public static void knockedPotComesOffAsItsDrops(GameTestHelper helper) {
        buildTable(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pot = helper.absolutePos(POT);
        helper.assertValueEqual(CatMischief.edgeDirection(level, pot), Direction.EAST, "only east has a drop and room for the cat");
        helper.assertTrue(CatMischief.edgeDirection(level, helper.absolutePos(POT.below())) == null, "planks aren't knockable");
        CatMischief.Target target = CatMischief.find(level, helper.absolutePos(CAT), EAGER.knockSearchRadius());
        helper.assertTrue(target != null && target.block().equals(pot), "the cat finds the pot");
        CatMischief.knock(level, target, null, null);
        assertPotAndPoppyOnce(helper);
        helper.succeed();
    }

    /** Pushing a block entity would lose its contents: none in the tag (and no tag entry is missing). */
    public static void catKnockableHasNoBlockEntities(GameTestHelper helper) {
        List<Block> blocks = new java.util.ArrayList<>();
        for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(FemboyTags.CAT_KNOCKABLE)) {
            blocks.add(holder.value());
        }
        helper.assertTrue(blocks.contains(Blocks.FLOWER_POT) && blocks.contains(Blocks.CANDLE), "pots and candles are in: " + blocks.size());
        for (Block block : blocks) {
            helper.assertFalse(block instanceof EntityBlock, block + " has a block entity");
        }
        helper.succeed();
    }

    /** Both rules; with a real cat, so on NeoForge the check goes through its mob griefing event (claim mods). */
    public static void catMischiefNeedsTheGameRules(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Cat cat = helper.spawnWithNoFreeWill(EntityType.CAT, CAT);
        helper.assertTrue(CatMischief.allowed(level, cat), "on by default");
        try {
            set(level, FemboyGameRules.CAT_MISCHIEF, false);
            helper.assertFalse(CatMischief.allowed(level, cat), "femboymod:cat_mischief off");
            set(level, FemboyGameRules.CAT_MISCHIEF, true);
            set(level, GameRules.RULE_MOBGRIEFING, false);
            helper.assertFalse(CatMischief.allowed(level, cat), "mob griefing off");
        } finally {
            set(level, FemboyGameRules.CAT_MISCHIEF, true);
            set(level, GameRules.RULE_MOBGRIEFING, true);
        }
        helper.succeed();
    }

    private static void set(ServerLevel level, GameRules.Key<GameRules.BooleanValue> rule, boolean value) {
        level.getGameRules().getRule(rule).set(value, level.getServer());
    }

    /** A real cat on the floor next to the table, a player watching: it climbs up and pushes the pot off. */
    public static void catKnocksAPotOffTheTable(GameTestHelper helper) {
        buildTable(helper);
        Cat cat = helper.spawnWithNoFreeWill(EntityType.CAT, CAT);
        ServerPlayer watcher = helper.makeMockServerPlayerInLevel();
        watcher.moveTo(helper.absoluteVec(Vec3.atBottomCenterOf(WATCHER)));
        CatKnockGoal goal = new CatKnockGoal(cat, () -> EAGER);
        int[] starts = {0};
        boolean[] running = {false};
        helper.onEachTick(() -> {
            if (!running[0]) {
                running[0] = goal.canUse();
                if (running[0]) {
                    starts[0]++;
                    goal.start();
                }
            } else if (goal.canContinueToUse()) {
                goal.tick();
            } else {
                goal.stop();
                running[0] = false;
            }
        });
        helper.succeedWhen(() -> {
            var nearest = helper.getLevel().getNearestPlayer(cat.getX(), cat.getY(), cat.getZ(), EAGER.knockAudienceRange(), e -> true);
            helper.assertTrue(helper.getBlockState(POT).isAir(), "the pot is still there (goal started " + starts[0]
                    + " times, cat at " + helper.relativePos(cat.blockPosition()) + ", path done " + cat.getNavigation().isDone()
                    + ", allowed " + CatMischief.allowed(helper.getLevel(), cat) + ", nearest " + nearest
                    + ", sees " + (nearest != null && nearest.hasLineOfSight(cat)) + ", sitting " + cat.isInSittingPose()
                    + " ordered " + cat.isOrderedToSit() + " lying " + cat.isLying()
                    + ", target " + CatMischief.find(helper.getLevel(), cat.blockPosition(), EAGER.knockSearchRadius()) + ")");
            assertPotAndPoppyOnce(helper);
            watcher.level().getServer().getPlayerList().remove(watcher);
        });
    }
}
