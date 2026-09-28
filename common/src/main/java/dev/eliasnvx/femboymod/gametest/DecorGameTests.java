package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.entity.StrayCat;
import dev.eliasnvx.femboymod.vibe.VibeScannerBlock;
import dev.eliasnvx.femboymod.vibe.VibeLeaderboard;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.effect.MobEffectInstance;
import dev.eliasnvx.femboymod.block.TerminalBlock;
import dev.eliasnvx.femboymod.food.BubbleTeaItem;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.block.FemboyBlocks;
import dev.eliasnvx.femboymod.block.GamerMonitorBlock;
import dev.eliasnvx.femboymod.block.LedStripBlock;
import dev.eliasnvx.femboymod.block.RubberDuckBlock;
import dev.eliasnvx.femboymod.block.SetupRating;
import dev.eliasnvx.femboymod.energy.FemboyEffects;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.entity.Seat;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import dev.eliasnvx.femboymod.world.FemboyWorldgen;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Gamer corner furniture, ores and posters. */
public final class DecorGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("gamer_chair_seats_and_heals", DecorGameTests::gamerChairSeatsAndHeals),
            new CosmeticGameTests.Entry("setup_rating_counts_kinds", DecorGameTests::setupRatingCountsKinds),
            new CosmeticGameTests.Entry("rubber_duck_grants_insight", DecorGameTests::rubberDuckGrantsInsight),
            new CosmeticGameTests.Entry("led_strip_cycles_colors", DecorGameTests::ledStripCyclesColors),
            new CosmeticGameTests.Entry("food_bubble_tea_and_strawberry_milk", DecorGameTests::foodBubbleTeaAndStrawberryMilk),
            new CosmeticGameTests.Entry("terminal_crafts_and_says_btw", DecorGameTests::terminalCraftsAndSaysBtw),
            new CosmeticGameTests.Entry("v11_clothing_slots_and_stats", DecorGameTests::v11ClothingSlotsAndStats),
            new CosmeticGameTests.Entry("vibe_scanner_scores_and_ranks", DecorGameTests::vibeScannerScoresAndRanks),
            new CosmeticGameTests.Entry("emotes_play_on_server", DecorGameTests::emotesPlayOnServer),
            new CosmeticGameTests.Entry("stray_cat_tames_and_brings_gifts", DecorGameTests::strayCatTamesAndBringsGifts),
            new CosmeticGameTests.Entry("cosplayer_sells_exclusive_colorways", DecorGameTests::cosplayerSellsExclusiveColorways),
            new CosmeticGameTests.Entry("geode_crystals_grow_and_new_ores_drop", DecorGameTests::geodeCrystalsGrowAndNewOresDrop),
            new CosmeticGameTests.Entry("dark_shades_impress_the_critic", DecorGameTests::darkShadesImpressTheCritic),
            new CosmeticGameTests.Entry("ores_generate_and_drop", DecorGameTests::oresGenerateAndDrop),
            new CosmeticGameTests.Entry("posters_and_glitter_colorway", DecorGameTests::postersAndGlitterColorway));

    private static final BlockPos CHAIR = new BlockPos(1, 1, 1);
    private static final BlockPos ORE = new BlockPos(2, 1, 1);

    private DecorGameTests() {
    }

    public static void gamerChairSeatsAndHeals(GameTestHelper helper) {
        helper.setBlock(CHAIR, FemboyBlocks.GAMER_CHAIR.get());
        BlockPos chair = helper.absolutePos(CHAIR);
        WearableGameTests.withPlayer(helper, player -> {
            helper.assertTrue(Seat.sit(helper.getLevel(), chair, player, 0.3), "player sits down");
            helper.assertTrue(player.getVehicle() instanceof Seat, "riding a seat");
            Seat seat = (Seat) player.getVehicle();
            helper.assertFalse(Seat.sit(helper.getLevel(), chair, player, 0.3), "one seat per chair");

            player.setHealth(10.0F);
            int interval = FemboyConfig.common().furniture().chairHealInterval();
            for (int i = 0; i < interval; i++) {
                seat.tick();
            }
            helper.assertTrue(player.getHealth() > 10.0F, "sitting heals (" + player.getHealth() + ")");

            helper.setBlock(CHAIR, Blocks.AIR);
            seat.tick();
            helper.assertTrue(seat.isRemoved(), "seat vanishes with the chair");
            helper.assertFalse(player.isPassenger(), "player stands up");
        });
    }

    /** Monitor (on), keyboard, LED, plush, duck and a poster: all stars; the monitor turned off loses one. */
    public static void setupRatingCountsKinds(GameTestHelper helper) {
        helper.setBlock(CHAIR, FemboyBlocks.GAMER_CHAIR.get());
        BlockPos chair = helper.absolutePos(CHAIR);
        int radius = FemboyConfig.common().furniture().setupRadius();
        helper.assertValueEqual(SetupRating.evaluate(helper.getLevel(), chair, radius), 0, "bare chair");
        helper.setBlock(CHAIR.offset(0, 1, 1), FemboyBlocks.GAMER_MONITOR.get());
        helper.setBlock(CHAIR.offset(1, 1, 1), FemboyBlocks.GAMER_KEYBOARD.get());
        helper.setBlock(CHAIR.offset(-1, 0, 1), FemboyBlocks.LED_STRIP.get());
        helper.setBlock(CHAIR.offset(2, 0, 0), FemboyBlocks.SHARK_PLUSH.get());
        helper.setBlock(CHAIR.offset(1, 1, 2), FemboyBlocks.RUBBER_DUCK.get());
        var poster = helper.getLevel().registryAccess().registryOrThrow(Registries.PAINTING_VARIANT)
                .getHolderOrThrow(ResourceKey.create(Registries.PAINTING_VARIANT, id("btw")));
        Painting painting = new Painting(helper.getLevel(), chair.offset(0, 2, 2), Direction.NORTH, poster);
        helper.getLevel().addFreshEntity(painting);
        helper.assertValueEqual(SetupRating.evaluate(helper.getLevel(), chair, radius), SetupRating.MAX, "full setup");
        helper.setBlock(CHAIR.offset(0, 1, 1), FemboyBlocks.GAMER_MONITOR.get().defaultBlockState().setValue(GamerMonitorBlock.LIT, false));
        helper.assertValueEqual(SetupRating.evaluate(helper.getLevel(), chair, radius), SetupRating.MAX - 1, "monitor off");
        painting.discard();
        helper.succeed();
    }

    public static void rubberDuckGrantsInsight(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            int xp = player.totalExperience;
            helper.assertTrue(RubberDuckBlock.debug(player), "first debugging session helps");
            helper.assertTrue(player.hasEffect(FemboyEffects.holder(FemboyEffects.INSIGHT)), "Insight granted");
            helper.assertTrue(player.totalExperience > xp, "a little experience");
            helper.assertFalse(RubberDuckBlock.debug(player), "cooldown: the duck needs a break too");
        });
    }

    public static void foodBubbleTeaAndStrawberryMilk(GameTestHelper helper) {
        helper.assertTrue(BubbleTeaItem.pick(helper.getLevel(), helper.getLevel().getRandom()) != null, "flavors loaded");
        WearableGameTests.withPlayer(helper, player -> {
            int before = player.getActiveEffects().size();
            ItemStack tea = new ItemStack(FemboyItems.BUBBLE_TEA.get());
            tea.finishUsingItem(helper.getLevel(), player);
            helper.assertTrue(player.getActiveEffects().size() > before, "a flavor buff was applied");
            // The mock player is in creative (keeps the item), so check the container the food component gives back
            helper.assertTrue(tea.get(DataComponents.FOOD).usingConvertsTo().filter(s -> s.is(Items.GLASS_BOTTLE)).isPresent(), "the bottle comes back");

            player.addEffect(new MobEffectInstance(FemboyEffects.holder(FemboyEffects.JITTER), 600));
            player.addEffect(new MobEffectInstance(FemboyEffects.holder(FemboyEffects.CAFFEINATED), 600));
            new ItemStack(FemboyItems.STRAWBERRY_MILK.get()).finishUsingItem(helper.getLevel(), player);
            helper.assertFalse(player.hasEffect(FemboyEffects.holder(FemboyEffects.JITTER)), "jitter gone");
            helper.assertFalse(player.hasEffect(FemboyEffects.holder(FemboyEffects.CAFFEINATED)), "no caffeine crash coming");
        });
    }

    public static void terminalCraftsAndSaysBtw(GameTestHelper helper) {
        helper.setBlock(CHAIR, FemboyBlocks.TERMINAL.get());
        WearableGameTests.withPlayer(helper, player -> {
            player.moveTo(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(CHAIR).above()));
            var menu = new TerminalBlock.Menu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(CHAIR)));
            helper.assertTrue(menu.stillValid(player), "Terminal works as a crafting table");
            helper.assertTrue(TerminalBlock.btw(player), "first btw");
            helper.assertFalse(TerminalBlock.btw(player), "cooldown: not every time");
            helper.setBlock(CHAIR, Blocks.CRAFTING_TABLE);
            helper.assertFalse(menu.stillValid(player), "menu closes when the Terminal is gone");
        });
    }

    /** Every v1.1 garment goes into its slot and adds Drip; the new effect types are registered. */
    public static void v11ClothingSlotsAndStats(GameTestHelper helper) {
        Map<Item, ResourceLocation> slots = Map.of(
                FemboyItems.CAT_EAR_HEADPHONES.get(), FemboySlots.HEAD_ACCESSORY,
                FemboyItems.HEART_GLASSES.get(), FemboySlots.FACE,
                FemboyItems.ARM_WARMERS.get(), FemboySlots.HANDS,
                FemboyItems.NAIL_POLISH.get(), FemboySlots.HANDS,
                FemboyItems.CROP_SWEATER.get(), FemboySlots.OUTFIT_TOP,
                FemboyItems.BELT_CHAINS.get(), FemboySlots.WAIST);
        helper.assertTrue(FemboyMod.api().cosmeticEffectTypes().get(id("glow_friends")).isPresent(), "glow_friends registered");
        helper.assertTrue(FemboyMod.api().cosmeticEffectTypes().get(id("muffle_sounds")).isPresent(), "muffle_sounds registered");
        WearableGameTests.withPlayer(helper, player -> slots.forEach((item, slot) -> {
            ItemStack stack = new ItemStack(item);
            helper.assertValueEqual(CosmeticsManager.slotOf(stack), slot, item + " slot");
            CosmeticsManager.set(player, slot, stack);
            helper.assertTrue(FemboyMod.api().getDripLevel(player).level() > 0, item + " adds drip");
            CosmeticsManager.set(player, slot, ItemStack.EMPTY);
        }));
    }

    public static void vibeScannerScoresAndRanks(GameTestHelper helper) {
        VibeLeaderboard board = new VibeLeaderboard();
        java.util.UUID a = java.util.UUID.randomUUID();
        java.util.UUID b = java.util.UUID.randomUUID();
        board.record(a, "a", 40, 2);
        helper.assertValueEqual(board.record(b, "b", 70, 2), 1, "higher score ranks first");
        helper.assertValueEqual(board.record(a, "a", 10, 2), 2, "a worse scan keeps the best");
        helper.assertValueEqual(board.entries().get(1).score(), 40, "best kept");
        board.record(java.util.UUID.randomUUID(), "c", 90, 2);
        helper.assertValueEqual(board.entries().size(), 2, "only the top N stay");

        WearableGameTests.withPlayer(helper, player -> {
            int bare = VibeScannerBlock.baseScore(player);
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
            helper.assertTrue(VibeScannerBlock.baseScore(player) > bare, "an outfit raises the vibe");
            int points = FemboyMod.api().getProfile(player).get(dev.eliasnvx.femboymod.api.profile.FemboyProfileFields.STYLE_POINTS);
            int score = VibeScannerBlock.scan(player);
            helper.assertTrue(score >= 0 && score <= 100, "score in range");
            int afterFirst = FemboyMod.api().getProfile(player).get(dev.eliasnvx.femboymod.api.profile.FemboyProfileFields.STYLE_POINTS);
            helper.assertTrue(afterFirst > points, "daily scan pays Style Points");
            VibeScannerBlock.scan(player);
            helper.assertValueEqual(FemboyMod.api().getProfile(player).get(dev.eliasnvx.femboymod.api.profile.FemboyProfileFields.STYLE_POINTS),
                    afterFirst, "only once per day");
            // the test world is reused between runs, so the board may already be full of better scans
            var entries = VibeLeaderboard.get(helper.getLevel().getServer()).entries();
            int size = dev.eliasnvx.femboymod.config.FemboyConfig.common().vibeCheck().leaderboardSize();
            helper.assertTrue(entries.stream().anyMatch(entry -> entry.player().equals(player.getUUID()))
                    || entries.size() == size && entries.getLast().score() >= score, "on the server leaderboard (or beaten by the whole top)");
        });
    }

    public static void emotesPlayOnServer(GameTestHelper helper) {
        helper.assertValueEqual(dev.eliasnvx.femboymod.emote.Emote.byId(99), dev.eliasnvx.femboymod.emote.Emote.WAVE, "unknown ids fall back");
        WearableGameTests.withPlayer(helper, player -> {
            for (dev.eliasnvx.femboymod.emote.Emote emote : dev.eliasnvx.femboymod.emote.Emote.values()) {
                dev.eliasnvx.femboymod.network.EmotePayloads.play(player, emote); // broadcast + particles must not throw
            }
        });
    }

    public static void strayCatTamesAndBringsGifts(GameTestHelper helper) {
        StrayCat cat = helper.spawn(dev.eliasnvx.femboymod.entity.FemboyEntities.STRAY_CAT.get(), CHAIR);
        cat.setCoat(7);
        helper.assertValueEqual(cat.coat(), 7 % StrayCat.COATS, "coat wraps to a valid pastel");
        WearableGameTests.withPlayer(helper, player -> {
            cat.tame(player);
            helper.assertTrue(cat.isTame() && cat.getOwner() == player, "tamed by the player");
            var area = new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(3);
            int before = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area).size();
            cat.giveGift(helper.getLevel(), player);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area).size() > before,
                    "a gift lands at the owner's feet");
            var kitten = cat.getBreedOffspring(helper.getLevel(), cat);
            helper.assertTrue(kitten instanceof StrayCat child && child.isTame(), "kittens are tame stray cats");
        });
    }

    public static void cosplayerSellsExclusiveColorways(GameTestHelper helper) {
        var cosplayer = helper.spawn(dev.eliasnvx.femboymod.entity.FemboyEntities.COSPLAYER.get(), CHAIR);
        var offers = cosplayer.getOffers();
        helper.assertTrue(offers.size() >= 4, "exclusive + common trades generated: " + offers.size());
        java.util.Set<String> exclusive = java.util.Set.of("sakura", "starlight", "cyber_pastel", "witchy");
        helper.assertTrue(offers.stream().anyMatch(offer -> {
            Colorway colorway = offer.getResult().get(FemboyComponents.COLORWAY.get());
            return colorway != null && colorway.pattern().map(p -> exclusive.contains(p.unwrapKey().orElseThrow().location().getPath())).orElse(false);
        }), "at least one exclusive colorway on sale");
        helper.succeed();
    }

    public static void geodeCrystalsGrowAndNewOresDrop(GameTestHelper helper) {
        var placed = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        for (var key : List.of(FemboyWorldgen.ORE_MOONSTONE, FemboyWorldgen.ORE_NEON_QUARTZ, FemboyWorldgen.ROSE_QUARTZ_GEODE)) {
            helper.assertTrue(placed.containsKey(key), key.location() + " loaded");
        }
        helper.setBlock(ORE, FemboyBlocks.BUDDING_ROSE_QUARTZ.get());
        helper.setBlock(ORE.above(), Blocks.AIR);
        List<Block> stages = List.of(FemboyBlocks.SMALL_ROSE_QUARTZ_BUD.get(), FemboyBlocks.MEDIUM_ROSE_QUARTZ_BUD.get(),
                FemboyBlocks.LARGE_ROSE_QUARTZ_BUD.get(), FemboyBlocks.ROSE_QUARTZ_CLUSTER.get());
        for (Block stage : stages) {
            dev.eliasnvx.femboymod.block.BuddingRoseQuartzBlock.grow(helper.getLevel(), helper.absolutePos(ORE), net.minecraft.core.Direction.UP);
            helper.assertTrue(helper.getBlockState(ORE.above()).is(stage), "grows into " + stage);
        }
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        Map<Block, Item> drops = Map.of(
                FemboyBlocks.DEEPSLATE_MOONSTONE_ORE.get(), FemboyItems.MOONSTONE.get(),
                FemboyBlocks.NEON_QUARTZ_ORE.get(), FemboyItems.NEON_QUARTZ.get(),
                FemboyBlocks.ROSE_QUARTZ_CLUSTER.get(), FemboyItems.ROSE_QUARTZ.get());
        drops.forEach((block, expected) -> {
            helper.setBlock(CHAIR, block);
            List<ItemStack> dropped = Block.getDrops(helper.getBlockState(CHAIR), helper.getLevel(), helper.absolutePos(CHAIR), null, null, pickaxe);
            helper.assertTrue(dropped.stream().anyMatch(stack -> stack.is(expected)), block + " drops " + expected + ": " + dropped);
        });
        helper.assertTrue(FemboyMod.api().cosmeticConditionTypes().get(id("night")).isPresent(), "night condition registered");
        helper.succeed();
    }

    /** Zero Drip but dark shades on: the critic is impressed (it gets weakened, the player is left alone). */
    public static void darkShadesImpressTheCritic(GameTestHelper helper) {
        var critic = helper.spawn(dev.eliasnvx.femboymod.entity.FemboyEntities.FASHION_CRITIC.get(), CHAIR);
        WearableGameTests.withPlayer(helper, player -> {
            CosmeticsManager.set(player, FemboySlots.FACE, new ItemStack(FemboyItems.DARK_SHADES.get()));
            critic.review(player);
            helper.assertFalse(player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN), "no bad review");
            helper.assertTrue(critic.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS), "the critic is impressed");
        });
    }

    public static void ledStripCyclesColors(GameTestHelper helper) {
        int color = 0;
        for (int i = 1; i < LedStripBlock.COLORS; i++) {
            color = LedStripBlock.nextColor(color, true);
            helper.assertValueEqual(color, i, "cycle step " + i);
        }
        helper.assertValueEqual(LedStripBlock.nextColor(LedStripBlock.RAINBOW, true), 0, "rainbow wraps to pink");
        helper.assertValueEqual(LedStripBlock.nextColor(LedStripBlock.RAINBOW - 1, false), 0, "rainbow skipped when disabled");
        helper.succeed();
    }

    public static void oresGenerateAndDrop(GameTestHelper helper) {
        var placed = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        helper.assertTrue(placed.containsKey(FemboyWorldgen.ORE_ROSE_QUARTZ), "rose quartz placed feature loaded");
        helper.assertTrue(placed.containsKey(FemboyWorldgen.ORE_GLITTER), "glitter placed feature loaded");

        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        Map<Block, Item> drops = Map.of(
                FemboyBlocks.ROSE_QUARTZ_ORE.get(), FemboyItems.ROSE_QUARTZ.get(),
                FemboyBlocks.DEEPSLATE_ROSE_QUARTZ_ORE.get(), FemboyItems.ROSE_QUARTZ.get(),
                FemboyBlocks.GLITTER_ORE.get(), FemboyItems.GLITTER.get());
        drops.forEach((ore, expected) -> {
            helper.setBlock(ORE, ore);
            List<ItemStack> dropped = Block.getDrops(helper.getBlockState(ORE), helper.getLevel(), helper.absolutePos(ORE), null, null, pickaxe);
            helper.assertTrue(dropped.stream().anyMatch(stack -> stack.is(expected)), ore + " drops " + expected + ": " + dropped);
            helper.assertTrue(helper.getBlockState(ORE).requiresCorrectToolForDrops(), ore + " needs a pickaxe");
        });
        helper.succeed();
    }

    public static void postersAndGlitterColorway(GameTestHelper helper) {
        var paintings = helper.getLevel().registryAccess().registryOrThrow(Registries.PAINTING_VARIANT);
        Map<String, int[]> sizes = Map.of("btw", new int[]{1, 1}, "stay_warm", new int[]{1, 2}, "code_with_love", new int[]{2, 2});
        sizes.forEach((name, size) -> {
            PaintingVariant variant = paintings.getOrThrow(ResourceKey.create(Registries.PAINTING_VARIANT, id(name)));
            helper.assertValueEqual(variant.width() + "x" + variant.height(), size[0] + "x" + size[1], name + " size");
        });

        var recipe = helper.getLevel().getRecipeManager().byKey(id("programming_socks_glitter"))
                .orElseThrow(() -> new GameTestAssertException("glitter recipe missing"));
        CraftingInput input = CraftingInput.of(3, 1, List.of(new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()),
                new ItemStack(FemboyItems.GLITTER.get()), new ItemStack(FemboyItems.GLITTER.get())));
        @SuppressWarnings("unchecked")
        Recipe<CraftingInput> crafting = (Recipe<CraftingInput>) recipe.value();
        helper.assertTrue(crafting instanceof CraftingRecipe && crafting.matches(input, helper.getLevel()), "socks + 2 glitter match");
        Colorway colorway = crafting.assemble(input, helper.getLevel().registryAccess()).get(FemboyComponents.COLORWAY.get());
        helper.assertTrue(colorway != null && colorway.hasShimmer(), "glitter socks shimmer: " + colorway);
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("femboymod", path);
    }
}
