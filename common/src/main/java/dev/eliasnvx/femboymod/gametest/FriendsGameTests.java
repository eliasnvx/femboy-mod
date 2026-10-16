package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.social.Headpats;
import dev.eliasnvx.femboymod.energy.FemboyEffects;
import dev.architectury.event.EventResult;
import com.mojang.authlib.GameProfile;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.entity.FemboyEntities;
import dev.eliasnvx.femboymod.entity.RubberDuck;
import dev.eliasnvx.femboymod.entity.SockGoblin;
import dev.eliasnvx.femboymod.halloween.TrickOrTreat;
import dev.eliasnvx.femboymod.pet.PetOutfitItem;
import dev.eliasnvx.femboymod.pet.PetOutfits;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import dev.eliasnvx.femboymod.world.FemboyGameRules;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;

/** 0.2.0 friends and fun: Halloween costume and trick-or-treat, pet outfits, Sock Goblin, Rubber Duck. */
public final class FriendsGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("halloween_pieces_wear_and_add_drip", FriendsGameTests::halloweenPiecesWearAndAddDrip),
            new CosmeticGameTests.Entry("trick_or_treat_once_per_villager_per_day", FriendsGameTests::trickOrTreatOncePerVillagerPerDay),
            new CosmeticGameTests.Entry("pet_outfits_on_and_off", FriendsGameTests::petOutfitsOnAndOff),
            new CosmeticGameTests.Entry("pet_outfits_only_for_the_owner", FriendsGameTests::petOutfitsOnlyForTheOwner),
            new CosmeticGameTests.Entry("pet_outfits_drop_once_on_death", FriendsGameTests::petOutfitsDropOnceOnDeath),
            new CosmeticGameTests.Entry("sock_goblin_steals_one_pair", FriendsGameTests::sockGoblinStealsOnePair),
            new CosmeticGameTests.Entry("sock_goblin_drops_loot_once", FriendsGameTests::sockGoblinDropsLootOnce),
            new CosmeticGameTests.Entry("rubber_duck_debugs_its_owner", FriendsGameTests::rubberDuckDebugsItsOwner),
            new CosmeticGameTests.Entry("new_mobs_run_their_ai", FriendsGameTests::newMobsRunTheirAi),
            new CosmeticGameTests.Entry("rubber_duck_floats_high", FriendsGameTests::rubberDuckFloatsHigh),
            new CosmeticGameTests.Entry("headpat_a_pet", FriendsGameTests::headpatAPet),
            new CosmeticGameTests.Entry("headpat_a_player", FriendsGameTests::headpatAPlayer));

    private static final BlockPos MOB_POS = new BlockPos(1, 2, 1);
    /** A pet that could use some healing. */
    private static final float HURT_HEALTH = 4.0F;
    /** Long enough for every goal to be tried a few times. */
    private static final int AI_TICKS = 60;
    /** A walled pool: water {@link #POOL_DEPTH} deep inside a {@link #POOL_SIZE} square, walls one block higher. */
    private static final int POOL_SIZE = 5;
    private static final int POOL_DEPTH = 2;
    /** How much of the duck may be under water once it settles (it is 0.5 tall; the toy floats high). */
    private static final double MIN_DRAFT = 0.05;
    private static final double MAX_DRAFT = 0.3;
    /** Settled: barely moving up or down. */
    private static final double MAX_BOB_SPEED = 0.02;
    /** Time to fall in and settle, then how long it is watched. */
    private static final int SPLASH_TICKS = 40;
    private static final int FLOAT_WINDOW_TICKS = 40;

    private FriendsGameTests() {
    }

    /**
     * Runs {@code body} with a logged-in survival player (the test's own mock player is in creative, where items
     * aren't used up), removed afterwards.
     */
    private static void withSurvivalPlayer(GameTestHelper helper, String name, java.util.function.Consumer<ServerPlayer> body) {
        var server = helper.getLevel().getServer();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(MOB_POS)));
        try {
            body.accept(player);
        } finally {
            server.getPlayerList().remove(player);
        }
    }

    private static List<ItemEntity> drops(GameTestHelper helper, BlockPos pos, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(4),
                entity -> entity.getItem().is(item));
    }

    public static void halloweenPiecesWearAndAddDrip(GameTestHelper helper) {
        Map<Item, Identifier> slots = Map.of(
                FemboyItems.WITCH_HAT.get(), FemboySlots.HEAD_ACCESSORY,
                FemboyItems.PUMPKIN_HOODIE.get(), FemboySlots.OUTFIT_TOP,
                FemboyItems.GHOST_SHEET.get(), FemboySlots.OUTFIT_TOP,
                FemboyItems.CANDY_PAIL.get(), FemboySlots.WAIST);
        WearableGameTests.withPlayer(helper, player -> {
            helper.assertFalse(TrickOrTreat.inCostume(player), "no costume yet");
            slots.forEach((item, slot) -> {
                ItemStack stack = new ItemStack(item);
                helper.assertValueEqual(CosmeticsManager.slotOf(stack), slot, item + " slot");
                CosmeticsManager.set(player, slot, stack);
                helper.assertTrue(FemboyMod.api().getDripLevel(player).level() > 0, item + " adds drip");
                helper.assertTrue(TrickOrTreat.inCostume(player), item + " is a costume");
                CosmeticsManager.set(player, slot, ItemStack.EMPTY);
            });
        });
    }

    public static void trickOrTreatOncePerVillagerPerDay(GameTestHelper helper) {
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, MOB_POS);
        withSurvivalPlayer(helper, "trick-or-treat", player -> {
            int candy = TrickOrTreat.treat(helper.getLevel(), player, villager);
            helper.assertTrue(candy > 0, "the villager gives candy");
            helper.assertValueEqual(player.getInventory().countItem(FemboyItems.CANDY.get()), candy, "candy in the inventory");
            helper.assertValueEqual(TrickOrTreat.treat(helper.getLevel(), player, villager), 0, "only once per day");
            helper.assertValueEqual(player.getInventory().countItem(FemboyItems.CANDY.get()), candy, "no more candy");
        });
        villager.discard();
        helper.succeed();
    }

    public static void petOutfitsOnAndOff(GameTestHelper helper) {
        Cat cat = helper.spawnWithNoFreeWill(EntityTypes.CAT, MOB_POS);
        withSurvivalPlayer(helper, "pet-owner", owner -> {
            cat.tame(owner);
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FemboyItems.PET_BOW.get()));
            PetOutfits.interact(owner, cat, InteractionHand.MAIN_HAND);
            helper.assertTrue(cat.getItemBySlot(EquipmentSlot.HEAD).is(FemboyItems.PET_BOW.get()), "the cat wears the bow");
            helper.assertTrue(owner.getMainHandItem().isEmpty(), "the bow left the hand (moved, not copied)");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FemboyItems.BELL_COLLAR.get()));
            PetOutfits.interact(owner, cat, InteractionHand.MAIN_HAND);
            helper.assertTrue(PetOutfits.worn(cat, PetOutfitItem.Slot.NECK).is(FemboyItems.BELL_COLLAR.get()), "and the collar");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FemboyItems.MINI_WITCH_HAT.get()));
            PetOutfits.interact(owner, cat, InteractionHand.MAIN_HAND);
            helper.assertTrue(cat.getItemBySlot(EquipmentSlot.HEAD).is(FemboyItems.MINI_WITCH_HAT.get()), "the hat replaces the bow");
            helper.assertValueEqual(owner.getInventory().countItem(FemboyItems.PET_BOW.get()), 1, "the bow came back");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
            PetOutfits.interact(owner, cat, InteractionHand.MAIN_HAND);
            helper.assertTrue(PetOutfits.worn(cat, PetOutfitItem.Slot.HEAD).isEmpty(), "shears take the hat off first");
            PetOutfits.interact(owner, cat, InteractionHand.MAIN_HAND);
            helper.assertTrue(PetOutfits.worn(cat, PetOutfitItem.Slot.NECK).isEmpty(), "then the collar");
            helper.assertValueEqual(owner.getInventory().countItem(FemboyItems.MINI_WITCH_HAT.get()), 1, "hat returned");
            helper.assertValueEqual(owner.getInventory().countItem(FemboyItems.BELL_COLLAR.get()), 1, "collar returned");
            cat.discard();
        });
        helper.succeed();
    }

    public static void petOutfitsOnlyForTheOwner(GameTestHelper helper) {
        Cat wild = helper.spawnWithNoFreeWill(EntityTypes.CAT, MOB_POS);
        withSurvivalPlayer(helper, "stranger", player -> withSurvivalPlayer(helper, "owner", owner -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FemboyItems.PET_BOW.get()));
            PetOutfits.interact(player, wild, InteractionHand.MAIN_HAND);
            helper.assertTrue(wild.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "wild cats don't wear outfits");
            wild.tame(owner);
            PetOutfits.interact(player, wild, InteractionHand.MAIN_HAND);
            helper.assertTrue(wild.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "only the owner dresses a pet");
            helper.assertTrue(player.getMainHandItem().is(FemboyItems.PET_BOW.get()), "the stranger keeps the bow");
        }));
        wild.discard();
        helper.succeed();
    }

    public static void petOutfitsDropOnceOnDeath(GameTestHelper helper) {
        Cat cat = helper.spawnWithNoFreeWill(EntityTypes.CAT, MOB_POS);
        withSurvivalPlayer(helper, "sad-owner", owner -> {
            cat.tame(owner);
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FemboyItems.PET_BANDANA.get()));
            PetOutfits.interact(owner, cat, InteractionHand.MAIN_HAND);
            cat.kill(helper.getLevel());
            List<ItemEntity> bandanas = drops(helper, MOB_POS, FemboyItems.PET_BANDANA.get());
            helper.assertValueEqual(bandanas.size(), 1, "exactly one bandana drops");
            bandanas.forEach(ItemEntity::discard);
        });
        helper.succeed();
    }

    public static void sockGoblinStealsOnePair(GameTestHelper helper) {
        SockGoblin goblin = helper.spawnWithNoFreeWill(FemboyEntities.SOCK_GOBLIN.get(), MOB_POS);
        withSurvivalPlayer(helper, "sock-owner", player -> {
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
            helper.assertFalse(goblin.canRob(player), "worn socks are safe");
            player.getInventory().add(new ItemStack(FemboyItems.FISHNET_TIGHTS.get()));
            helper.assertTrue(goblin.canRob(player), "carried legwear is fair game");
            helper.assertTrue(goblin.steal(player), "snatched");
            helper.assertTrue(goblin.getMainHandItem().is(FemboyItems.FISHNET_TIGHTS.get()), "in the goblin's hand");
            helper.assertValueEqual(player.getInventory().countItem(FemboyItems.FISHNET_TIGHTS.get()), 0, "gone from the inventory");
            helper.assertTrue(goblin.isPersistenceRequired(), "never despawns with someone's socks");
            helper.assertFalse(goblin.steal(player), "one pair at a time");
            helper.assertTrue(CosmeticsManager.get(player).get(FemboySlots.LEGS_OVERLAY).is(FemboyItems.PROGRAMMING_SOCKS.get()),
                    "worn socks stay on");
            goblin.discard();
            SockGoblin polite = helper.spawnWithNoFreeWill(FemboyEntities.SOCK_GOBLIN.get(), MOB_POS);
            player.getInventory().add(new ItemStack(FemboyItems.FISHNET_TIGHTS.get()));
            var rules = helper.getLevel().getGameRules();
            rules.set(FemboyGameRules.SOCK_GOBLIN_THEFT.get(), false, helper.getLevel().getServer());
            try {
                helper.assertFalse(polite.canRob(player), "game rule off: no theft");
            } finally {
                rules.set(FemboyGameRules.SOCK_GOBLIN_THEFT.get(), true, helper.getLevel().getServer());
            }
            polite.discard();
        });
        helper.succeed();
    }

    public static void sockGoblinDropsLootOnce(GameTestHelper helper) {
        SockGoblin goblin = helper.spawnWithNoFreeWill(FemboyEntities.SOCK_GOBLIN.get(), MOB_POS);
        withSurvivalPlayer(helper, "chaser", player -> {
            player.getInventory().add(new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
            goblin.steal(player);
            goblin.kill(helper.getLevel());
            List<ItemEntity> socks = drops(helper, MOB_POS, FemboyItems.PROGRAMMING_SOCKS.get());
            List<ItemEntity> lost = drops(helper, MOB_POS, FemboyItems.LOST_SOCK.get());
            helper.assertValueEqual(socks.size(), 1, "the stolen socks come back once");
            helper.assertValueEqual(lost.size(), 1, "plus a lost sock");
            socks.forEach(ItemEntity::discard);
            lost.forEach(ItemEntity::discard);
        });
        helper.succeed();
    }

    public static void rubberDuckDebugsItsOwner(GameTestHelper helper) {
        RubberDuck duck = helper.spawnWithNoFreeWill(FemboyEntities.RUBBER_DUCK.get(), MOB_POS);
        withSurvivalPlayer(helper, "coder", owner -> {
            duck.tame(owner);
            owner.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
            helper.assertTrue(duck.debug(helper.getLevel(), owner) != null, "the duck fixes a bug");
            helper.assertFalse(owner.hasEffect(MobEffects.POISON), "poison is gone");
            owner.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
            helper.assertTrue(duck.debug(helper.getLevel(), owner) == null, "the duck needs a break too");
            helper.assertTrue(owner.hasEffect(MobEffects.POISON), "still poisoned during the cooldown");
            duck.discard();
        });
        helper.succeed();
    }

    /**
     * The new mobs with their AI on, near a player holding duck food and carrying socks: every goal gets to run
     * (a missing attribute, like tempt_range for TemptGoal, crashes the server here instead of in a world).
     */
    public static void newMobsRunTheirAi(GameTestHelper helper) {
        RubberDuck duck = helper.spawn(FemboyEntities.RUBBER_DUCK.get(), MOB_POS);
        RubberDuck duckling = helper.spawn(FemboyEntities.RUBBER_DUCK.get(), MOB_POS.east());
        duckling.setAge(-24000);
        SockGoblin goblin = helper.spawn(FemboyEntities.SOCK_GOBLIN.get(), MOB_POS.south());
        var player = helper.makeMockServerPlayerInLevel();
        player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(MOB_POS.north())));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS));
        player.getInventory().add(new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
        helper.runAfterDelay(AI_TICKS, () -> {
            helper.assertTrue(duck.isAlive() && duckling.isAlive() && goblin.isAlive(), "all still around");
            player.level().getServer().getPlayerList().remove(player);
            duck.discard();
            duckling.discard();
            goblin.discard();
            helper.succeed();
        });
    }

    /** Dropped into water, the duck settles high on the surface (most of it dry) instead of bobbing half under. */
    public static void rubberDuckFloatsHigh(GameTestHelper helper) {
        int wallTop = POOL_DEPTH + 2;
        for (int x = 0; x < POOL_SIZE; x++) {
            for (int z = 0; z < POOL_SIZE; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                boolean wall = x == 0 || z == 0 || x == POOL_SIZE - 1 || z == POOL_SIZE - 1;
                for (int y = 2; y <= wallTop; y++) {
                    helper.setBlock(new BlockPos(x, y, z), wall ? Blocks.STONE : y <= POOL_DEPTH + 1 ? Blocks.WATER : Blocks.AIR);
                }
            }
        }
        BlockPos middle = new BlockPos(POOL_SIZE / 2, wallTop, POOL_SIZE / 2);
        RubberDuck duck = helper.spawn(FemboyEntities.RUBBER_DUCK.get(), middle);
        // over a window after the splash: the deepest it sits and the fastest it bobs (vanilla floating bobs on a
        // cycle of ~16 ticks, so one sample could miss it)
        double[] deepest = {0.0};
        double[] fastest = {0.0};
        helper.onEachTick(() -> {
            if (helper.getTick() >= SPLASH_TICKS && duck.isAlive()) {
                deepest[0] = Math.max(deepest[0], duck.getFluidHeight(FluidTags.WATER));
                fastest[0] = Math.max(fastest[0], Math.abs(duck.getDeltaMovement().y));
            }
        });
        helper.runAfterDelay(SPLASH_TICKS + FLOAT_WINDOW_TICKS, () -> {
            double draft = duck.getFluidHeight(FluidTags.WATER);
            helper.assertTrue(duck.isInWater(), "the duck is on the water");
            helper.assertTrue(draft >= MIN_DRAFT && deepest[0] <= MAX_DRAFT, "the duck floats high, draft " + draft
                    + ", deepest " + deepest[0]);
            helper.assertTrue(fastest[0] < MAX_BOB_SPEED, "the duck has settled, fastest bob " + fastest[0]);
            duck.discard();
            helper.succeed();
        });
    }

    /** Sneak + empty hand on a tamed cat: Cozy and a bit of health; standing or holding something does nothing. */
    public static void headpatAPet(GameTestHelper helper) {
        var owner = helper.makeMockServerPlayerInLevel();
        Cat cat = helper.spawn(EntityTypes.CAT, MOB_POS);
        cat.tame(owner);
        cat.setHealth(HURT_HEALTH);
        helper.assertTrue(Headpats.interact(owner, cat, InteractionHand.MAIN_HAND) == EventResult.pass(), "standing: no headpat");
        owner.setShiftKeyDown(true);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertTrue(Headpats.interact(owner, cat, InteractionHand.MAIN_HAND) == EventResult.pass(), "holding a stick: no headpat");
        helper.assertFalse(cat.hasEffect(FemboyEffects.COZY.asHolder()), "not Cozy yet");
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Headpats.interact(owner, cat, InteractionHand.MAIN_HAND);
        helper.assertTrue(cat.hasEffect(FemboyEffects.COZY.asHolder()), "the cat is Cozy");
        helper.assertTrue(cat.getHealth() > HURT_HEALTH, "and a little healed");
        RubberDuck duck = helper.spawn(FemboyEntities.RUBBER_DUCK.get(), MOB_POS.east());
        duck.tame(owner);
        helper.assertFalse(Headpats.canPat(duck), "rubber ducks keep their sneak-click (sit)");
        owner.level().getServer().getPlayerList().remove(owner);
        cat.discard();
        duck.discard();
        helper.succeed();
    }

    /** One player pats another: Cozy; patting again right away is on cooldown. */
    public static void headpatAPlayer(GameTestHelper helper) {
        var patter = helper.makeMockServerPlayerInLevel();
        var friend = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(Headpats.pat(helper.getLevel(), patter, friend), "the headpat lands");
        helper.assertTrue(friend.hasEffect(FemboyEffects.COZY.asHolder()), "the friend is Cozy");
        helper.assertFalse(Headpats.pat(helper.getLevel(), patter, friend), "right away again: cooldown");
        patter.level().getServer().getPlayerList().remove(patter);
        friend.level().getServer().getPlayerList().remove(friend);
        helper.succeed();
    }
}
