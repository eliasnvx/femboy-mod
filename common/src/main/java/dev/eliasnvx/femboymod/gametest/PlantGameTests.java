package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.block.DebugFlytrapBlock;
import dev.eliasnvx.femboymod.block.FemboyBlocks;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.energy.FemboyEffects;
import dev.eliasnvx.femboymod.entity.CatKnockGoal;
import dev.eliasnvx.femboymod.entity.Catnip;
import dev.eliasnvx.femboymod.entity.CatnipGoal;
import dev.eliasnvx.femboymod.entity.FemboyEntities;
import dev.eliasnvx.femboymod.entity.StrayCat;
import dev.eliasnvx.femboymod.social.TouchGrass;
import dev.eliasnvx.femboymod.world.FemboyWorldgen;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Plants (0.3.0): the Debug Flytrap, potted plants, touching grass, catnip and cats. On 1.20.1 the flower pot check
 * also proves potting works on both loaders: vanilla's content map on Fabric, Forge 47's {@code addPlant} on Forge.
 */
public final class PlantGameTests {

    /** The flytrap's first check comes {@code plants.flytrap_check_interval} ticks after it's placed (also the Fabric glue's timeout). */
    public static final int FLYTRAP_TICKS = 60;

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("flytrap_eats_a_bug_and_grows", PlantGameTests::flytrapEatsABugAndGrows, FLYTRAP_TICKS),
            new CosmeticGameTests.Entry("flytrap_reach_and_prey", PlantGameTests::flytrapReachAndPrey),
            new CosmeticGameTests.Entry("plants_go_into_flower_pots", PlantGameTests::plantsGoIntoFlowerPots),
            new CosmeticGameTests.Entry("touching_grass_buffs_once_and_resets_the_timer", PlantGameTests::touchingGrassBuffsOnce),
            new CosmeticGameTests.Entry("catnip_phases_zoomies_then_purring", PlantGameTests::catnipPhasesZoomiesThenPurring),
            new CosmeticGameTests.Entry("catnip_fed_tames_stray_cats", PlantGameTests::catnipFedTamesStrayCats),
            new CosmeticGameTests.Entry("catnip_high_cats_knock_more", PlantGameTests::catnipHighCatsKnockMore),
            new CosmeticGameTests.Entry("catnip_spreads_with_bone_meal", PlantGameTests::catnipSpreadsWithBoneMeal),
            new CosmeticGameTests.Entry("plants_are_registered_for_worldgen", PlantGameTests::plantsAreRegisteredForWorldgen));

    private static final BlockPos TRAP = new BlockPos(1, 2, 1);
    private static final long LONG_SESSION = 72000;

    private PlantGameTests() {
    }

    private static void grassFloor(GameTestHelper helper, int size) {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }
    }

    /** A real flytrap loop: placing it schedules the check, it eats the bug next to it, then digests and grows. */
    public static void flytrapEatsABugAndGrows(GameTestHelper helper) {
        grassFloor(helper, 4);
        helper.setBlock(TRAP, FemboyBlocks.DEBUG_FLYTRAP.get());
        var bug = helper.spawnWithNoFreeWill(FemboyEntities.BUG.get(), TRAP.east());
        helper.succeedWhen(() -> {
            helper.assertFalse(bug.isAlive(), "the bug is still there");
            BlockState state = helper.getBlockState(TRAP);
            helper.assertTrue(state.getValue(DebugFlytrapBlock.DIGESTING), "jaws closed while digesting");
            GameTestAsserts.assertValueEqual(helper, state.getValue(DebugFlytrapBlock.STAGE), 0, "no growth before digesting");
            // skip the wait: run the digest tick now
            ServerLevel level = helper.getLevel();
            state.tick(level, helper.absolutePos(TRAP), level.getRandom());
            BlockState fed = helper.getBlockState(TRAP);
            helper.assertFalse(fed.getValue(DebugFlytrapBlock.DIGESTING), "jaws open again");
            GameTestAsserts.assertValueEqual(helper, fed.getValue(DebugFlytrapBlock.STAGE), 1, "one meal, one stage");
            helper.assertTrue(level.getBlockTicks().hasScheduledTick(helper.absolutePos(TRAP), fed.getBlock()), "hunting again");
        });
    }

    /** Reach grows with the stage; named bugs and non-prey are left alone; silverfish count as bugs; no bone meal. */
    public static void flytrapReachAndPrey(GameTestHelper helper) {
        grassFloor(helper, 6);
        CommonConfig.Plants config = FemboyConfig.common().plants();
        ServerLevel level = helper.getLevel();
        BlockPos trap = helper.absolutePos(TRAP);
        var far = helper.spawnWithNoFreeWill(FemboyEntities.BUG.get(), TRAP.east(3));
        helper.assertTrue(DebugFlytrapBlock.findPrey(level, trap, DebugFlytrapBlock.reach(0, config)) == null, "3 blocks is out of a seedling's reach");
        helper.assertTrue(DebugFlytrapBlock.findPrey(level, trap, DebugFlytrapBlock.reach(DebugFlytrapBlock.MAX_STAGE, config)) == far,
                "a grown flytrap reaches it");
        far.setCustomName(Component.literal("feature"));
        helper.assertTrue(DebugFlytrapBlock.findPrey(level, trap, DebugFlytrapBlock.reach(DebugFlytrapBlock.MAX_STAGE, config)) == null,
                "named bugs are features");
        helper.spawnWithNoFreeWill(EntityType.PIG, TRAP.south());
        helper.assertTrue(DebugFlytrapBlock.findPrey(level, trap, DebugFlytrapBlock.reach(0, config)) == null, "pigs aren't prey");
        var silverfish = helper.spawnWithNoFreeWill(EntityType.SILVERFISH, TRAP.north());
        helper.assertTrue(DebugFlytrapBlock.findPrey(level, trap, DebugFlytrapBlock.reach(0, config)) == silverfish, "silverfish are bugs too");
        helper.assertFalse(FemboyBlocks.DEBUG_FLYTRAP.get() instanceof BonemealableBlock, "bone meal does nothing");
        helper.succeed();
    }

    /** Flytrap and catnip go into a flower pot like vanilla plants; Potted Grass keeps its grass on an empty-hand click. */
    public static void plantsGoIntoFlowerPots(GameTestHelper helper) {
        BlockPos pot = new BlockPos(1, 2, 1);
        helper.setBlock(pot.below(), Blocks.STONE);
        ServerPlayer player = InteriorGameTests.survivalPlayer(helper, "gardener", pot.east(2));
        try {
            potWith(helper, player, pot, new ItemStack(FemboyBlocks.DEBUG_FLYTRAP.get()), FemboyBlocks.POTTED_DEBUG_FLYTRAP.get().defaultBlockState());
            helper.assertTrue(helper.getLevel().getBlockTicks().hasScheduledTick(helper.absolutePos(pot), FemboyBlocks.POTTED_DEBUG_FLYTRAP.get()),
                    "the potted flytrap hunts too");
            potWith(helper, player, pot, new ItemStack(FemboyBlocks.CATNIP.get()), FemboyBlocks.POTTED_CATNIP.get().defaultBlockState());
            potWith(helper, player, pot, new ItemStack(Items.GRASS), FemboyBlocks.POTTED_GRASS.get().defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            use(helper, player, pot);
            helper.assertBlockPresent(FemboyBlocks.POTTED_GRASS.get(), pot);
            helper.assertTrue(player.getMainHandItem().isEmpty(), "touching doesn't pull the grass out");
            helper.assertTrue(player.hasEffect(MobEffects.REGENERATION), "touched the potted grass");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            use(helper, player, pot);
            helper.assertBlockPresent(FemboyBlocks.POTTED_GRASS.get(), pot); // 1.20.1's pot would hand the grass out here
            helper.assertTrue(player.getMainHandItem().is(Items.STICK) && player.getInventory().countItem(Items.GRASS) == 0,
                    "a click with a stick doesn't pull the grass out either");
            for (var potted : List.of(FemboyBlocks.POTTED_DEBUG_FLYTRAP, FemboyBlocks.POTTED_CATNIP, FemboyBlocks.POTTED_GRASS)) {
                helper.assertTrue(potted.get().defaultBlockState().is(BlockTags.FLOWER_POTS), potted.getId() + " in #minecraft:flower_pots");
            }
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    private static void potWith(GameTestHelper helper, ServerPlayer player, BlockPos pot, ItemStack plant, BlockState expected) {
        helper.setBlock(pot, Blocks.FLOWER_POT);
        player.setItemInHand(InteractionHand.MAIN_HAND, plant);
        use(helper, player, pot);
        helper.assertTrue(helper.getBlockState(pot) == expected, "potted " + plant + ": got " + helper.getBlockState(pot));
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the plant went into the pot");
    }

    /** Right-click through the game mode, so loader events (touching grass) fire like for a real click. */
    private static void use(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    /** Touching grass resets the timer and buffs, but the buff waits for its cooldown; holding something doesn't count. */
    public static void touchingGrassBuffsOnce(GameTestHelper helper) {
        BlockPos grass = new BlockPos(1, 1, 1);
        helper.setBlock(grass, Blocks.GRASS_BLOCK);
        ServerPlayer player = InteriorGameTests.survivalPlayer(helper, "grass_toucher", grass.above().east(2));
        boolean online = true;
        try {
            helper.assertTrue(TouchGrass.session(player) != null, "the timer starts on join");
            long now = helper.getLevel().getServer().getTickCount();
            TouchGrass.resetTimer(player, now - LONG_SESSION); // pretend they've been playing for a while
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            use(helper, player, grass);
            helper.assertFalse(player.hasEffect(MobEffects.REGENERATION), "a stick isn't a hand");
            GameTestAsserts.assertValueEqual(helper, TouchGrass.session(player).since(), now - LONG_SESSION, "no reset with a stick");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            use(helper, player, grass);
            helper.assertTrue(player.hasEffect(MobEffects.REGENERATION), "touched grass: Regeneration");
            GameTestAsserts.assertValueEqual(helper, TouchGrass.session(player).since(), now, "timer reset");
            player.removeEffect(MobEffects.REGENERATION);
            helper.assertFalse(TouchGrass.touch(helper.getLevel(), player, helper.absolutePos(grass)), "no second buff right away");
            helper.assertFalse(player.hasEffect(MobEffects.REGENERATION), "the lawn isn't a healing station");
            // a relog ends the session but not the buff cooldown
            helper.getLevel().getServer().getPlayerList().remove(player);
            online = false;
            helper.assertTrue(TouchGrass.session(player) == null, "the timer ends with the session");
            helper.assertFalse(TouchGrass.touch(helper.getLevel(), player, helper.absolutePos(grass)), "rejoining doesn't refresh the buff");
            TouchGrass.forget(player);
        } finally {
            if (online) {
                helper.getLevel().getServer().getPlayerList().remove(player);
            }
        }
        helper.succeed();
    }

    /** Smelling catnip starts the high: zoomies with a speed boost, later lying down purring; more catnip waits. */
    public static void catnipPhasesZoomiesThenPurring(GameTestHelper helper) {
        grassFloor(helper, 5);
        helper.setBlock(new BlockPos(3, 2, 3), FemboyBlocks.CATNIP.get());
        Cat cat = helper.spawnWithNoFreeWill(EntityType.CAT, new BlockPos(1, 2, 1));
        CatnipGoal goal = new CatnipGoal(cat);
        boolean started = false;
        for (int i = 0; i < 40 && !started; i++) { // sniffs once a second, spread by entity id
            started = goal.canUse();
        }
        helper.assertTrue(started, "the cat smelled the catnip");
        GameTestAsserts.assertValueEqual(helper, Catnip.phase(cat), Catnip.Phase.ZOOMIES, "zoomies first");
        goal.start();
        helper.assertTrue(cat.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(CatnipGoal.ZOOMIES_SPEED) != null, "zooming fast");
        helper.assertFalse(Catnip.start(cat), "no second high on top");
        CommonConfig.Plants config = FemboyConfig.common().plants();
        setHigh(cat, config.catnipToleranceTicks() + config.catnipPurrTicks());
        helper.assertFalse(goal.canContinueToUse(), "zoomies over");
        goal.stop();
        helper.assertTrue(cat.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(CatnipGoal.ZOOMIES_SPEED) == null, "speed back to normal");
        helper.assertTrue(goal.canUse(), "purring next");
        goal.start();
        helper.assertTrue(cat.isLying(), "lying down purring");
        setHigh(cat, config.catnipToleranceTicks());
        helper.assertFalse(goal.canContinueToUse(), "done purring");
        goal.stop();
        helper.assertFalse(cat.isLying(), "up again");
        GameTestAsserts.assertValueEqual(helper, Catnip.phase(cat), Catnip.Phase.TOLERANT, "ignores catnip for a while");
        helper.assertFalse(Catnip.start(cat), "still tolerant");
        helper.succeed();
    }

    private static void setHigh(Cat cat, int remaining) {
        cat.removeEffect(FemboyEffects.holder(FemboyEffects.CATNIP));
        cat.addEffect(new MobEffectInstance(FemboyEffects.holder(FemboyEffects.CATNIP), remaining, 0, false, false, false));
    }

    /** Feeding catnip uses one sprig and starts the high; a Stray Cat can get tamed by it; a high cat isn't interested. */
    public static void catnipFedTamesStrayCats(GameTestHelper helper) {
        grassFloor(helper, 5);
        ServerPlayer player = InteriorGameTests.survivalPlayer(helper, "catnip_feeder", new BlockPos(3, 2, 3));
        try {
            StrayCat lucky = helper.spawnWithNoFreeWill(FemboyEntities.STRAY_CAT.get(), new BlockPos(1, 2, 1));
            StrayCat shy = helper.spawnWithNoFreeWill(FemboyEntities.STRAY_CAT.get(), new BlockPos(1, 2, 3));
            ItemStack catnip = new ItemStack(FemboyBlocks.CATNIP.get(), 3);
            helper.assertTrue(Catnip.feed(helper.getLevel(), lucky, player, catnip, 1.0F), "the cat ate it");
            helper.assertTrue(lucky.isTame() && lucky.isOwnedBy(player), "catnip won it over");
            helper.assertFalse(lucky.isOrderedToSit(), "no sitting down: zoomies come first");
            helper.assertTrue(Catnip.feed(helper.getLevel(), shy, player, catnip, 0.0F), "the shy one ate too");
            helper.assertFalse(shy.isTame(), "but stays wild this time");
            GameTestAsserts.assertValueEqual(helper, catnip.getCount(), 1, "one sprig each");
            helper.assertFalse(Catnip.feed(helper.getLevel(), lucky, player, catnip, 1.0F), "already high: not interested");
            GameTestAsserts.assertValueEqual(helper, catnip.getCount(), 1, "nothing used up");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    /** A cat that never knocks on its own (chance 0) still goes for the pot in the mischief part of a catnip high. */
    public static void catnipHighCatsKnockMore(GameTestHelper helper) {
        CatMischiefGameTests.buildTable(helper);
        Cat cat = helper.spawnWithNoFreeWill(EntityType.CAT, CatMischiefGameTests.CAT);
        ServerPlayer watcher = InteriorGameTests.survivalPlayer(helper, "catnip_watcher", CatMischiefGameTests.WATCHER);
        try {
            CommonConfig.Cats never = new CommonConfig.Cats(72000, 0.0F, 0, 6, 12.0, 5);
            helper.assertFalse(new CatKnockGoal(cat, () -> never).canUse(), "a sober cat with chance 0 doesn't");
            CommonConfig.Plants config = FemboyConfig.common().plants();
            setHigh(cat, config.catnipToleranceTicks() + config.catnipPurrTicks() + config.catnipMischiefTicks());
            GameTestAsserts.assertValueEqual(helper, Catnip.phase(cat), Catnip.Phase.MISCHIEF, "mischief phase");
            helper.assertTrue(new CatKnockGoal(cat, () -> never).canUse(), "a catnip-high cat goes for it");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(watcher);
        }
        helper.succeed();
    }

    public static void catnipSpreadsWithBoneMeal(GameTestHelper helper) {
        grassFloor(helper, 5);
        BlockPos center = new BlockPos(2, 2, 2);
        helper.setBlock(center, FemboyBlocks.CATNIP.get());
        ServerLevel level = helper.getLevel();
        BlockState state = helper.getBlockState(center);
        BonemealableBlock catnip = (BonemealableBlock) state.getBlock();
        helper.assertTrue(catnip.isValidBonemealTarget(level, helper.absolutePos(center), state, false), "room to spread");
        catnip.performBonemeal(level, level.getRandom(), helper.absolutePos(center), state);
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(0, 2, 0), new BlockPos(4, 3, 4))) {
            if (helper.getBlockState(pos).is(FemboyBlocks.CATNIP.get())) {
                count++;
            }
        }
        GameTestAsserts.assertValueEqual(helper, count, 2, "one new catnip");
        helper.succeed();
    }

    public static void plantsAreRegisteredForWorldgen(GameTestHelper helper) {
        var placed = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        helper.assertTrue(placed.containsKey(FemboyWorldgen.PATCH_DEBUG_FLYTRAP), "flytrap patch loaded");
        helper.assertTrue(placed.containsKey(FemboyWorldgen.PATCH_CATNIP), "catnip patch loaded");
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        helper.assertTrue(biomes.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.SWAMP).is(FemboyWorldgen.HAS_DEBUG_FLYTRAP), "swamps have flytraps");
        helper.assertTrue(biomes.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.MEADOW).is(FemboyWorldgen.HAS_CATNIP), "meadows have catnip");
        helper.succeed();
    }
}
