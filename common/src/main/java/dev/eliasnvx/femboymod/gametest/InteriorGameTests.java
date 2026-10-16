package dev.eliasnvx.femboymod.gametest;

import com.mojang.authlib.GameProfile;
import dev.eliasnvx.femboymod.block.DyedFurniture;
import dev.eliasnvx.femboymod.block.FairyLightsBlock;
import dev.eliasnvx.femboymod.block.FemboyBlocks;
import dev.eliasnvx.femboymod.block.FluffyRugBlock;
import dev.eliasnvx.femboymod.block.LaundryBasketBlockEntity;
import dev.eliasnvx.femboymod.block.LavaLampBlock;
import dev.eliasnvx.femboymod.block.MiniFridgeBlockEntity;
import dev.eliasnvx.femboymod.block.MirrorBlock;
import dev.eliasnvx.femboymod.block.MirrorBlockEntity;
import dev.eliasnvx.femboymod.energy.EnergyDrinks;
import dev.eliasnvx.femboymod.entity.FemboyEntities;
import dev.eliasnvx.femboymod.entity.SockGoblin;
import dev.eliasnvx.femboymod.menu.CosmeticsMenu;
import dev.eliasnvx.femboymod.menu.FurnitureStorageMenu;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import dev.eliasnvx.femboymod.registry.FemboyMenus;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 0.2.0 cozy interior: dyeable furniture, seats, lamps, the pet bed (cats and dogs), the laundry basket (filter, drops,
 * the goblin's first target) and the mini fridge (filter, chilling), the vanity table's mirror.
 */
public final class InteriorGameTests {

    /** Pets look for a bed every 2 s and heal every 3 s; the goblin walks over to the basket. */
    public static final int PET_TICKS = 600;
    public static final int GOBLIN_TICKS = 400;

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("bean_bag_seats_and_dyes", InteriorGameTests::beanBagSeatsAndDyes),
            new CosmeticGameTests.Entry("dyed_furniture_drops_its_color", InteriorGameTests::dyedFurnitureDropsItsColor),
            new CosmeticGameTests.Entry("fluffy_rug_shapes_and_support", InteriorGameTests::fluffyRugShapesAndSupport),
            new CosmeticGameTests.Entry("fairy_lights_need_a_wall", InteriorGameTests::fairyLightsNeedAWall),
            new CosmeticGameTests.Entry("lava_lamp_switches_and_dyes", InteriorGameTests::lavaLampSwitchesAndDyes),
            new CosmeticGameTests.Entry("pet_bed_cat_lies_down_and_heals", InteriorGameTests::petBedCatLiesDownAndHeals, PET_TICKS),
            new CosmeticGameTests.Entry("pet_bed_dog_sits_and_heals", InteriorGameTests::petBedDogSitsAndHeals, PET_TICKS),
            new CosmeticGameTests.Entry("pet_beds_one_pet_each", InteriorGameTests::petBedsOnePetEach, PET_TICKS),
            new CosmeticGameTests.Entry("laundry_basket_takes_only_clothes", InteriorGameTests::laundryBasketTakesOnlyClothes),
            new CosmeticGameTests.Entry("laundry_basket_contents_drop_once", InteriorGameTests::laundryBasketContentsDropOnce),
            new CosmeticGameTests.Entry("sock_goblin_raids_the_basket_first", InteriorGameTests::sockGoblinRaidsTheBasketFirst, GOBLIN_TICKS),
            new CosmeticGameTests.Entry("mini_fridge_chills_byte_energy", InteriorGameTests::miniFridgeChillsByteEnergy),
            new CosmeticGameTests.Entry("vanity_table_is_a_mirror", InteriorGameTests::vanityTableIsAMirror));

    private static final BlockPos POS = new BlockPos(1, 1, 1);
    /** Room for tests where mobs walk: FLOOR x FLOOR blocks of stone at y 0, walled in (the test structure is 8x8x8). */
    private static final int FLOOR = 8;
    /** Pets start this far from the bed. */
    private static final BlockPos PET_START = new BlockPos(5, 1, 5);
    private static final BlockPos BED = new BlockPos(2, 1, 2);
    private static final BlockPos OWNER_SPOT = new BlockPos(5, 1, 2);
    private static final float HURT_HEALTH = 4.0F;
    /** Menu index of hotbar slot 0: 9 storage slots, then 27 inventory slots. */
    private static final int HOTBAR_0 = FurnitureStorageMenu.SIZE + 27;

    private InteriorGameTests() {
    }

    public static void beanBagSeatsAndDyes(GameTestHelper helper) {
        helper.setBlock(POS, FemboyBlocks.BEAN_BAG.get());
        withSurvivalPlayer(helper, player -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DyeItem.byColor(DyeColor.LIGHT_BLUE), 2));
            helper.useBlock(POS, player);
            helper.assertValueEqual(helper.getBlockState(POS).getValue(DyedFurniture.COLOR), DyeColor.LIGHT_BLUE, "dyed light blue");
            helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "one dye used up");
            helper.useBlock(POS, player);
            helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "same color again uses no dye");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            helper.useBlock(POS, player);
            helper.assertTrue(player.isPassenger(), "sitting on the bean bag");
            player.stopRiding();
        });
        helper.succeed();
    }

    public static void dyedFurnitureDropsItsColor(GameTestHelper helper) {
        BlockState blue = FemboyBlocks.PET_BED.get().defaultBlockState().setValue(DyedFurniture.COLOR, DyeColor.BLUE);
        helper.setBlock(POS, blue);
        helper.getLevel().destroyBlock(helper.absolutePos(POS), true);
        List<ItemEntity> drops = drops(helper, FemboyBlocks.PET_BED.get().asItem());
        helper.assertValueEqual(drops.size(), 1, "the pet bed dropped");
        BlockItemStateProperties properties = drops.getFirst().getItem().get(DataComponents.BLOCK_STATE);
        helper.assertTrue(properties != null && properties.get(DyedFurniture.COLOR) == DyeColor.BLUE, "the drop is still blue: " + properties);
        drops.forEach(ItemEntity::discard);
        helper.succeed();
    }

    public static void fluffyRugShapesAndSupport(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.STONE);
        helper.setBlock(POS.above(), FemboyBlocks.FLUFFY_RUG.get());
        WearableGameTests.withPlayer(helper, player -> {
            helper.useBlock(POS.above(), player);
            helper.assertValueEqual(helper.getBlockState(POS.above()).getValue(FluffyRugBlock.SHAPE), FluffyRugBlock.Shape.ROUND, "round");
            helper.useBlock(POS.above(), player);
            helper.assertValueEqual(helper.getBlockState(POS.above()).getValue(FluffyRugBlock.SHAPE), FluffyRugBlock.Shape.HEART, "heart");
            helper.setBlock(POS, Blocks.AIR);
            helper.assertBlockPresent(Blocks.AIR, POS.above());
        });
    }

    public static void fairyLightsNeedAWall(GameTestHelper helper) {
        BlockPos wall = POS.south();
        helper.setBlock(wall, Blocks.STONE);
        BlockState lights = FemboyBlocks.FAIRY_LIGHTS.get().defaultBlockState().setValue(FairyLightsBlock.FACING, Direction.NORTH);
        helper.assertTrue(lights.canSurvive(helper.getLevel(), helper.absolutePos(POS)), "hangs on the wall to the south");
        helper.assertFalse(lights.setValue(FairyLightsBlock.FACING, Direction.SOUTH).canSurvive(helper.getLevel(), helper.absolutePos(POS)),
                "nothing to hang on to the north");
        helper.setBlock(POS, lights);
        helper.assertValueEqual(helper.getBlockState(POS).getLightEmission(), FairyLightsBlock.LIGHT, "gives light");
        WearableGameTests.withPlayer(helper, player -> {
            helper.useBlock(POS, player);
            helper.assertValueEqual(helper.getBlockState(POS).getValue(FairyLightsBlock.MODE), 1, "next bulbs");
            helper.setBlock(wall, Blocks.AIR);
            helper.assertBlockPresent(Blocks.AIR, POS);
        });
    }

    public static void lavaLampSwitchesAndDyes(GameTestHelper helper) {
        helper.setBlock(POS, FemboyBlocks.LAVA_LAMP.get());
        helper.assertValueEqual(helper.getBlockState(POS).getLightEmission(), LavaLampBlock.LIGHT, "on when placed");
        WearableGameTests.withPlayer(helper, player -> {
            helper.useBlock(POS, player);
            helper.assertValueEqual(helper.getBlockState(POS).getLightEmission(), 0, "switched off");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DyeItem.byColor(DyeColor.LIME)));
            helper.useBlock(POS, player);
            helper.assertValueEqual(helper.getBlockState(POS).getValue(DyedFurniture.COLOR), DyeColor.LIME, "lime wax");
            helper.assertFalse(helper.getBlockState(POS).getValue(LavaLampBlock.LIT), "dyeing doesn't switch it");
        });
    }

    /** A walled room for walking mobs (nobody wanders off the floor) with a pet bed in it. */
    private static void bedroom(GameTestHelper helper) {
        for (int x = 0; x < FLOOR; x++) {
            for (int z = 0; z < FLOOR; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                boolean wall = x == 0 || z == 0 || x == FLOOR - 1 || z == FLOOR - 1;
                helper.setBlock(new BlockPos(x, 1, z), wall ? Blocks.STONE : Blocks.AIR);
                helper.setBlock(new BlockPos(x, 2, z), wall ? Blocks.STONE : Blocks.AIR);
            }
        }
        helper.setBlock(BED, FemboyBlocks.PET_BED.get());
    }

    public static void petBedCatLiesDownAndHeals(GameTestHelper helper) {
        bedroom(helper);
        ServerPlayer owner = owner(helper);
        Cat cat = helper.spawn(EntityType.CAT, PET_START);
        cat.tame(owner);
        cat.setHealth(HURT_HEALTH);
        helper.succeedWhen(() -> {
            helper.assertTrue(cat.isLying() && cat.blockPosition().equals(helper.absolutePos(BED)), "the cat lies on the bed");
            helper.assertTrue(cat.getHealth() > HURT_HEALTH, "and heals");
            cat.discard();
            owner.level().getServer().getPlayerList().remove(owner);
        });
    }

    public static void petBedDogSitsAndHeals(GameTestHelper helper) {
        bedroom(helper);
        ServerPlayer owner = owner(helper);
        Wolf wolf = helper.spawn(EntityType.WOLF, PET_START);
        wolf.tame(owner);
        wolf.setHealth(HURT_HEALTH);
        helper.succeedWhen(() -> {
            helper.assertTrue(wolf.isInSittingPose() && wolf.blockPosition().equals(helper.absolutePos(BED)), "the dog sits on the bed");
            helper.assertTrue(wolf.getHealth() > HURT_HEALTH, "and heals");
            wolf.discard();
            owner.level().getServer().getPlayerList().remove(owner);
        });
    }

    /** A cat and a dog, two beds: each takes its own (nobody pushes the other off). */
    public static void petBedsOnePetEach(GameTestHelper helper) {
        bedroom(helper);
        BlockPos secondBed = new BlockPos(5, 1, 5);
        helper.setBlock(secondBed, FemboyBlocks.PET_BED.get());
        ServerPlayer owner = owner(helper);
        Cat cat = helper.spawn(EntityType.CAT, new BlockPos(3, 1, 4));
        Wolf wolf = helper.spawn(EntityType.WOLF, new BlockPos(4, 1, 3));
        cat.tame(owner);
        wolf.tame(owner);
        helper.succeedWhen(() -> {
            helper.assertTrue(cat.isLying() && wolf.isInSittingPose(), "both rest");
            helper.assertFalse(cat.blockPosition().equals(wolf.blockPosition()), "on different beds");
            cat.discard();
            wolf.discard();
            owner.level().getServer().getPlayerList().remove(owner);
        });
    }

    public static void laundryBasketTakesOnlyClothes(GameTestHelper helper) {
        helper.setBlock(POS, FemboyBlocks.LAUNDRY_BASKET.get());
        LaundryBasketBlockEntity basket = helper.getBlockEntity(POS);
        helper.assertFalse(basket.canPlaceItem(0, new ItemStack(Items.DIRT)), "no dirt in the laundry");
        helper.assertTrue(basket.canPlaceItem(0, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get())), "socks go in");
        WearableGameTests.withPlayer(helper, player -> {
            FurnitureStorageMenu menu = new FurnitureStorageMenu(FemboyMenus.LAUNDRY_BASKET.get(), 1, player.getInventory(), basket);
            player.getInventory().setItem(0, new ItemStack(Items.DIRT, 5));
            menu.quickMoveStack(player, HOTBAR_0);
            helper.assertValueEqual(player.getInventory().getItem(0).getCount(), 5, "dirt stays in the inventory");
            helper.assertTrue(basket.isEmpty(), "basket still empty");
            player.getInventory().setItem(0, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
            menu.quickMoveStack(player, HOTBAR_0);
            helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "socks left the inventory");
            helper.assertValueEqual(basket.countItem(FemboyItems.PROGRAMMING_SOCKS.get()), 1, "socks in the basket");
            menu.removed(player);
        });
    }

    public static void laundryBasketContentsDropOnce(GameTestHelper helper) {
        helper.setBlock(POS, FemboyBlocks.LAUNDRY_BASKET.get());
        LaundryBasketBlockEntity basket = helper.getBlockEntity(POS);
        basket.setItem(0, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
        basket.setItem(1, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
        helper.getLevel().destroyBlock(helper.absolutePos(POS), true);
        List<ItemEntity> socks = drops(helper, FemboyItems.PROGRAMMING_SOCKS.get());
        helper.assertValueEqual(socks.stream().mapToInt(item -> item.getItem().getCount()).sum(), 2, "both socks dropped, once");
        helper.assertValueEqual(drops(helper, FemboyBlocks.LAUNDRY_BASKET.get().asItem()).size(), 1, "and the basket");
        socks.forEach(ItemEntity::discard);
        drops(helper, FemboyBlocks.LAUNDRY_BASKET.get().asItem()).forEach(ItemEntity::discard);
        helper.succeed();
    }

    public static void sockGoblinRaidsTheBasketFirst(GameTestHelper helper) {
        bedroom(helper);
        BlockPos basketPos = new BlockPos(5, 1, 2);
        helper.setBlock(basketPos, FemboyBlocks.LAUNDRY_BASKET.get());
        LaundryBasketBlockEntity basket = helper.getBlockEntity(basketPos);
        basket.setItem(0, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
        ServerPlayer player = survivalPlayer(helper, "laundry_owner", new BlockPos(2, 1, 5));
        player.getInventory().add(new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
        SockGoblin goblin = helper.spawn(FemboyEntities.SOCK_GOBLIN.get(), new BlockPos(3, 1, 3));
        helper.succeedWhen(() -> {
            helper.assertTrue(goblin.hasLoot(), "the goblin took socks");
            helper.assertTrue(basket.isEmpty(), "from the basket");
            helper.assertValueEqual(player.getInventory().countItem(FemboyItems.PROGRAMMING_SOCKS.get()), 1, "the player keeps theirs");
            goblin.discard();
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
    }

    public static void miniFridgeChillsByteEnergy(GameTestHelper helper) {
        helper.setBlock(POS, FemboyBlocks.MINI_FRIDGE.get());
        MiniFridgeBlockEntity fridge = helper.getBlockEntity(POS);
        helper.assertFalse(fridge.canPlaceItem(0, new ItemStack(Items.DIRT)), "no dirt in the fridge");
        ItemStack can = new ItemStack(FemboyItems.BYTE_ENERGY_PINK.get());
        helper.assertTrue(fridge.canPlaceItem(0, can), "Byte Energy goes in");
        fridge.setItem(0, can);
        fridge.setItem(1, new ItemStack(FemboyItems.MOCHI.get()));
        fridge.chill(2);
        helper.assertFalse(fridge.getItem(0).has(FemboyComponents.CHILLED.get()), "not cold after one tick");
        fridge.chill(2);
        helper.assertTrue(fridge.getItem(0).has(FemboyComponents.CHILLED.get()), "cold after two");
        helper.assertFalse(fridge.getItem(1).has(FemboyComponents.CHILLED.get()), "mochi isn't chilled");
        ItemStack chilled = fridge.getItem(0).copy();
        WearableGameTests.withPlayer(helper, warmDrinker -> {
            EnergyDrinks.onDrink(helper.getLevel(), warmDrinker, new ItemStack(FemboyItems.BYTE_ENERGY_PINK.get()));
            int warm = longestBuff(warmDrinker);
            warmDrinker.removeAllEffects();
            EnergyDrinks.onDrink(helper.getLevel(), warmDrinker, chilled);
            helper.assertTrue(longestBuff(warmDrinker) > warm, "chilled buffs last longer: " + longestBuff(warmDrinker) + " vs " + warm);
            warmDrinker.removeAllEffects();
        });
    }

    public static void vanityTableIsAMirror(GameTestHelper helper) {
        BlockState lower = FemboyBlocks.VANITY_TABLE.get().defaultBlockState().setValue(MirrorBlock.FACING, Direction.NORTH);
        helper.setBlock(POS, lower);
        helper.setBlock(POS.above(), lower.setValue(MirrorBlock.HALF, DoubleBlockHalf.UPPER));
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(POS)) instanceof MirrorBlockEntity,
                "the table half draws reflections");
        WearableGameTests.withPlayer(helper, player -> {
            helper.useBlock(POS.above(), player);
            helper.assertTrue(player.containerMenu instanceof CosmeticsMenu, "Outfit screen open, got " + player.containerMenu);
            player.closeContainer();
            helper.setBlock(POS, Blocks.AIR);
            helper.assertBlockPresent(Blocks.AIR, POS.above());
        });
    }

    /** The pet's owner, standing nearby: a tamed pet without an owner just sits (vanilla sit goal). */
    private static ServerPlayer owner(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.moveTo(helper.absoluteVec(Vec3.atBottomCenterOf(OWNER_SPOT)));
        return owner;
    }

    private static int longestBuff(ServerPlayer player) {
        return player.getActiveEffects().stream().mapToInt(MobEffectInstance::getDuration).max().orElse(0);
    }

    private static List<ItemEntity> drops(GameTestHelper helper, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(POS)).inflate(3),
                entity -> entity.getItem().is(item));
    }

    /** A connected survival player (mock players are creative: dyes and socks would not be used up or stolen). */
    private static ServerPlayer survivalPlayer(GameTestHelper helper, String name, BlockPos pos) {
        var server = helper.getLevel().getServer();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(Vec3.atBottomCenterOf(pos)));
        return player;
    }

    private static void withSurvivalPlayer(GameTestHelper helper, java.util.function.Consumer<ServerPlayer> body) {
        ServerPlayer player = survivalPlayer(helper, "furniture_tester", POS.east(2));
        try {
            body.accept(player);
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
    }
}
