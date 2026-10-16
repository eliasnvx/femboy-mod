package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.FemboyCreativeTabs;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.colorway.ColorwayPattern;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsEvents;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.menu.CosmeticsMenu;
import dev.eliasnvx.femboymod.network.CosmeticsSyncPayload;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Loader-independent GameTest bodies (SPEC §13). Registered by
 * {@code fabric/src/gametest} (Fabric) and {@code FemboyGameTestsForge} (Forge).
 */
public final class CosmeticGameTests {

    /** Test name -> body. Loader glue iterates this list, so adding a test here registers it everywhere. */
    public static final List<Entry> ALL = List.of(
            new Entry("equip_and_unequip", CosmeticGameTests::equipAndUnequip),
            new Entry("right_click_equip_swaps", CosmeticGameTests::rightClickEquipSwaps),
            new Entry("quick_move_conserves_items", CosmeticGameTests::quickMoveConservesItems),
            new Entry("death_drops_cosmetics", CosmeticGameTests::deathDropsCosmetics),
            new Entry("attachment_codec_round_trip", CosmeticGameTests::attachmentCodecRoundTrip),
            new Entry("sync_payload_round_trip", CosmeticGameTests::syncPayloadRoundTrip),
            new Entry("colorway_patterns_loaded", CosmeticGameTests::colorwayPatternsLoaded),
            new Entry("creative_tab_contains_items", CosmeticGameTests::creativeTabContainsItems));

    private static final ResourceLocation HEAD = FemboySlots.HEAD_ACCESSORY;
    private static final Vec3 TEST_AREA_CENTER = new Vec3(1.5, 1.0, 1.5);

    private CosmeticGameTests() {
    }

    /** Every shared test of femboymod (all Phase suites). */
    public static List<Entry> all() {
        List<Entry> all = new java.util.ArrayList<>(ALL);
        all.addAll(WearableGameTests.ALL);
        all.addAll(BackpackGameTests.ALL);
        all.addAll(WorldGameTests.ALL);
        all.addAll(MobGameTests.ALL);
        all.addAll(PanelGameTests.ALL);
        all.addAll(DecorGameTests.ALL);
        all.addAll(ProfileGameTests.ALL);
        all.addAll(LookbookGameTests.ALL);
        all.addAll(FeetSlotGameTests.ALL);
        all.addAll(MirrorGameTests.ALL);
        all.addAll(PhotoGameTests.ALL);
        all.addAll(ExternalSlotsGameTests.ALL);
        all.addAll(FriendsGameTests.ALL);
        all.addAll(InteriorGameTests.ALL);
        return all;
    }

    /**
     * A shared test: its name, body and how many ticks it may take (loaders without per-test annotations read it;
     * Fabric's {@code @GameTest(timeoutTicks)} must match).
     */
    public record Entry(String name, Consumer<GameTestHelper> body, int maxTicks) {

        public static final int DEFAULT_MAX_TICKS = 100;

        public Entry(String name, Consumer<GameTestHelper> body) {
            this(name, body, DEFAULT_MAX_TICKS);
        }
    }

    public static void equipAndUnequip(GameTestHelper helper) {
        withPlayer(helper, player -> {
            ItemStack ears = new ItemStack(FemboyItems.CAT_EARS.get());
            helper.assertTrue(CosmeticsManager.canEquip(player, HEAD, ears), "Cat ears must fit the head slot");
            helper.assertFalse(CosmeticsManager.canEquip(player, FemboySlots.NECK, ears), "Cat ears must not fit the neck slot");
            helper.assertFalse(CosmeticsManager.canEquip(player, HEAD, new ItemStack(Items.STICK)), "A stick is not a cosmetic");

            CosmeticsManager.set(player, HEAD, ears);
            helper.assertTrue(FemboyApi.get().getCosmetics(player).get(HEAD).is(FemboyItems.CAT_EARS.get()), "Ears should be worn");

            CosmeticsManager.set(player, HEAD, ItemStack.EMPTY);
            helper.assertTrue(FemboyApi.get().getCosmetics(player).isEmpty(), "Slots should be empty after unequip");
        });
    }

    public static void rightClickEquipSwaps(GameTestHelper helper) {
        withPlayer(helper, player -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FemboyItems.CAT_EARS.get()));
            CosmeticsEvents.equipFromHand(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getMainHandItem().isEmpty(), "Hand should be empty after equipping");
            helper.assertTrue(CosmeticsManager.get(player).get(HEAD).is(FemboyItems.CAT_EARS.get()), "Ears should be worn");

            ItemStack pinkEars = new ItemStack(FemboyItems.CAT_EARS.get());
            FemboyComponents.COLORWAY.set(pinkEars, Colorway.solid(0xFFB6D9));
            player.setItemInHand(InteractionHand.MAIN_HAND, pinkEars);
            CosmeticsEvents.equipFromHand(player, InteractionHand.MAIN_HAND);

            helper.assertTrue(FemboyComponents.COLORWAY.has(CosmeticsManager.get(player).get(HEAD)), "New ears should be worn");
            helper.assertTrue(player.getMainHandItem().is(FemboyItems.CAT_EARS.get())
                    && !FemboyComponents.COLORWAY.has(player.getMainHandItem()), "Old ears should come back to the hand");
            assertCount(helper, player, FemboyItems.CAT_EARS.get(), 2);
        });
    }

    public static void quickMoveConservesItems(GameTestHelper helper) {
        withPlayer(helper, player -> {
            CosmeticsMenu menu = new CosmeticsMenu(1, player.getInventory());
            int cosmeticSlots = CosmeticsManager.orderedSlots().size();
            int headIndex = CosmeticsManager.orderedSlots().indexOf(HEAD);
            int firstHotbar = cosmeticSlots + 27;

            player.getInventory().setItem(0, new ItemStack(FemboyItems.CAT_EARS.get()));
            menu.quickMoveStack(player, firstHotbar);
            helper.assertTrue(menu.getSlot(headIndex).getItem().is(FemboyItems.CAT_EARS.get()), "Shift-click should equip");
            assertCount(helper, player, FemboyItems.CAT_EARS.get(), 1);

            menu.quickMoveStack(player, headIndex);
            helper.assertTrue(CosmeticsManager.get(player).isEmpty(), "Shift-click should unequip");
            assertCount(helper, player, FemboyItems.CAT_EARS.get(), 1);

            player.getInventory().setItem(1, new ItemStack(Items.STICK));
            helper.assertTrue(menu.quickMoveStack(player, firstHotbar + 1).isEmpty(), "Non-cosmetics must not move into slots");
            helper.assertTrue(player.getInventory().getItem(1).is(Items.STICK), "Stick must stay put");
            helper.assertTrue(menu.getSlot(headIndex).mayPlace(new ItemStack(FemboyItems.CAT_EARS.get())), "Head slot accepts ears");
            helper.assertFalse(menu.getSlot(headIndex).mayPlace(new ItemStack(Items.STICK)), "Head slot rejects sticks");
        });
    }

    public static void deathDropsCosmetics(GameTestHelper helper) {
        withPlayer(helper, player -> {
            CosmeticsManager.set(player, HEAD, new ItemStack(FemboyItems.CAT_EARS.get()));
            CosmeticsEvents.dropOnDeath(player);
            helper.assertTrue(CosmeticsManager.get(player).isEmpty(), "Slots should be cleared on death");

            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3),
                    item -> item.getItem().is(FemboyItems.CAT_EARS.get()));
            GameTestAsserts.assertValueEqual(helper, drops.size(), 1, "Exactly one ears drop");
            drops.forEach(Entity::discard);
        });
    }

    public static void attachmentCodecRoundTrip(GameTestHelper helper) {
        CosmeticInventory inventory = sampleInventory(helper);
        var ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
        Tag tag = CosmeticInventory.CODEC.encodeStart(ops, inventory).getOrThrow(false, error -> { });
        CosmeticInventory decoded = CosmeticInventory.CODEC.parse(ops, tag).getOrThrow(false, error -> { });
        assertSame(helper, inventory, decoded);
        helper.succeed();
    }

    public static void syncPayloadRoundTrip(GameTestHelper helper) {
        CosmeticsSyncPayload payload = new CosmeticsSyncPayload(42, sampleInventory(helper), false);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            payload.write(buf);
            CosmeticsSyncPayload decoded = CosmeticsSyncPayload.read(buf);
            GameTestAsserts.assertValueEqual(helper, decoded.entityId(), 42, "entity id");
            assertSame(helper, payload.cosmetics(), decoded.cosmetics());
            helper.assertFalse(decoded.armorHidingAllowed(), "armor hiding flag");
        } finally {
            buf.release();
        }
        helper.succeed();
    }

    public static void colorwayPatternsLoaded(GameTestHelper helper) {
        Registry<ColorwayPattern> registry = helper.getLevel().registryAccess().registryOrThrow(ColorwayPattern.REGISTRY_KEY);
        Holder<ColorwayPattern> trans = registry.getHolderOrThrow(ResourceKey.create(ColorwayPattern.REGISTRY_KEY,
                new ResourceLocation(FemboyApi.MOD_ID, "pride_trans")));
        GameTestAsserts.assertValueEqual(helper, trans.value().stripes().size(), 5, "trans flag stripes");
        helper.assertTrue(registry.size() >= 12, "All built-in patterns should load, got " + registry.size());
        helper.succeed();
    }

    public static void creativeTabContainsItems(GameTestHelper helper) {
        CreativeModeTabs.tryRebuildTabContents(FeatureFlags.DEFAULT_FLAGS, true, helper.getLevel().registryAccess());
        CreativeModeTab tab = FemboyItems.TAB.get();
        List<CreativeModeTab> tabs = List.of(tab, FemboyItems.ROOM_TAB.get(), FemboyItems.SNACKS_TAB.get(),
                FemboyItems.MATERIALS_TAB.get(), FemboyItems.MOBS_TAB.get());
        helper.assertTrue(CreativeModeTabs.allTabs().containsAll(tabs), "femboymod tabs are registered");
        // the API keys name these tabs, and their ids sort in menu order (Fabric and Forge 1.20.1 order mod tabs by id)
        List<ResourceKey<CreativeModeTab>> keys = List.of(FemboyCreativeTabs.OUTFITS, FemboyCreativeTabs.ROOM,
                FemboyCreativeTabs.SNACKS, FemboyCreativeTabs.MATERIALS, FemboyCreativeTabs.MOBS);
        for (int i = 0; i < tabs.size(); i++) {
            GameTestAsserts.assertValueEqual(helper, BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tabs.get(i)).orElse(null),
                    keys.get(i), "tab key");
            helper.assertTrue(i == 0 || keys.get(i - 1).location().compareTo(keys.get(i).location()) < 0,
                    "tab ids sort in menu order: " + keys.get(i).location());
        }
        // each item where a player would look for it
        helper.assertTrue(tab.contains(new ItemStack(FemboyItems.CAT_EARS.get())), "Cat Ears in the outfits tab");
        helper.assertTrue(tab.contains(new ItemStack(FemboyItems.LOOKBOOK.get())), "the Lookbook in the outfits tab");
        helper.assertTrue(tabs.get(1).contains(new ItemStack(dev.eliasnvx.femboymod.block.FemboyBlocks.BEAN_BAG.get())),
                "the Bean Bag in the room tab");
        helper.assertTrue(tabs.get(2).contains(new ItemStack(FemboyItems.BYTE_ENERGY_PINK.get())), "Byte Energy in the snacks tab");
        helper.assertTrue(tabs.get(3).contains(new ItemStack(FemboyItems.ROSE_QUARTZ.get())), "rose quartz in the materials tab");
        helper.assertTrue(tabs.get(4).contains(new ItemStack(FemboyItems.PINK_CREEPER_SPAWN_EGG.get())), "spawn eggs in the mobs tab");
        helper.assertTrue(tabs.get(4).contains(new ItemStack(FemboyItems.PET_BOW.get())), "pet outfits in the mobs tab");
        // every item exactly once over all tabs (plain stacks; colorway and dye color presets come on top)
        java.util.Map<Item, Integer> seen = new java.util.HashMap<>();
        for (CreativeModeTab each : tabs) {
            each.getDisplayItems().stream()
                    .filter(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(FemboyApi.MOD_ID))
                    .filter(stack -> !FemboyComponents.COLORWAY.has(stack) && stack.getTagElement(BlockItem.BLOCK_STATE_TAG) == null)
                    .forEach(stack -> seen.merge(stack.getItem(), 1, Integer::sum));
        }
        BuiltInRegistries.ITEM.stream()
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(FemboyApi.MOD_ID))
                .forEach(item -> GameTestAsserts.assertValueEqual(helper, seen.getOrDefault(item, 0), 1,
                        "tabs showing " + BuiltInRegistries.ITEM.getKey(item)));
        long stripedSocks = tab.getDisplayItems().stream()
                .filter(stack -> stack.is(FemboyItems.PROGRAMMING_SOCKS.get()))
                .filter(stack -> FemboyComponents.COLORWAY.has(stack) && FemboyComponents.COLORWAY.get(stack).stripeCount() == 2)
                .count();
        GameTestAsserts.assertValueEqual(helper, stripedSocks, 3L, "black, blue and red striped socks next to the pink ones");
        helper.succeed();
    }

    private static CosmeticInventory sampleInventory(GameTestHelper helper) {
        Registry<ColorwayPattern> patterns = helper.getLevel().registryAccess().registryOrThrow(ColorwayPattern.REGISTRY_KEY);
        ItemStack ears = new ItemStack(FemboyItems.CAT_EARS.get());
        FemboyComponents.COLORWAY.set(ears, new Colorway(0xFFB6D9,
                Optional.of(patterns.getHolderOrThrow(ResourceKey.create(ColorwayPattern.REGISTRY_KEY,
                        new ResourceLocation(FemboyApi.MOD_ID, "pride_bi")))),
                Optional.of(0x123456)));
        return CosmeticInventory.EMPTY.with(HEAD, ears)
                .withArmor(net.minecraft.world.entity.EquipmentSlot.HEAD, dev.eliasnvx.femboymod.api.cosmetic.ArmorVisibility.SHOW)
                .withArmor(net.minecraft.world.entity.EquipmentSlot.FEET, dev.eliasnvx.femboymod.api.cosmetic.ArmorVisibility.HIDE);
    }

    private static void assertSame(GameTestHelper helper, CosmeticInventory expected, CosmeticInventory actual) {
        GameTestAsserts.assertValueEqual(helper, actual.all().keySet(), expected.all().keySet(), "slots");
        GameTestAsserts.assertValueEqual(helper, actual.armor(), expected.armor(), "armor visibility");
        expected.all().forEach((slot, stack) ->
                helper.assertTrue(ItemStack.matches(stack, actual.get(slot)), "Stack in " + slot + " should survive the round trip"));
    }

    private static void assertCount(GameTestHelper helper, Player player, Item item, int expected) {
        int count = player.getInventory().countItem(item);
        for (ItemStack worn : CosmeticsManager.get(player).all().values()) {
            if (worn.is(item)) {
                count += worn.getCount();
            }
        }
        GameTestAsserts.assertValueEqual(helper, count, expected, "total " + item + " (inventory + worn)");
    }

    /** Runs the body with a fresh mock player and always removes the player afterwards. */
    private static void withPlayer(GameTestHelper helper, Consumer<ServerPlayer> body) {
        ServerPlayer player = MockPlayers.create(helper);
        // The mock player joins at world spawn; move it into the (loaded) test area.
        player.moveTo(helper.absoluteVec(TEST_AREA_CENTER));
        try {
            body.accept(player);
            helper.succeed();
        } finally {
            player.level().getServer().getPlayerList().remove(player);
        }
    }
}
