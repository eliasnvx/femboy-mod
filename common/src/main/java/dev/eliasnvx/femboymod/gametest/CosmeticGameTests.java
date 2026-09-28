package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.api.FemboyApi;
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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
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
 * {@code fabric/src/gametest} (Fabric) and {@code FemboyGameTestsNeoForge} (NeoForge).
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
        return all;
    }

    public record Entry(String name, Consumer<GameTestHelper> body) {
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
            pinkEars.set(FemboyComponents.COLORWAY.get(), Colorway.solid(0xFFB6D9));
            player.setItemInHand(InteractionHand.MAIN_HAND, pinkEars);
            CosmeticsEvents.equipFromHand(player, InteractionHand.MAIN_HAND);

            helper.assertTrue(CosmeticsManager.get(player).get(HEAD).has(FemboyComponents.COLORWAY.get()), "New ears should be worn");
            helper.assertTrue(player.getMainHandItem().is(FemboyItems.CAT_EARS.get())
                    && !player.getMainHandItem().has(FemboyComponents.COLORWAY.get()), "Old ears should come back to the hand");
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
            helper.assertValueEqual(drops.size(), 1, "Exactly one ears drop");
            drops.forEach(Entity::discard);
        });
    }

    public static void attachmentCodecRoundTrip(GameTestHelper helper) {
        CosmeticInventory inventory = sampleInventory(helper);
        var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag tag = CosmeticInventory.CODEC.encodeStart(ops, inventory).getOrThrow();
        CosmeticInventory decoded = CosmeticInventory.CODEC.parse(ops, tag).getOrThrow();
        assertSame(helper, inventory, decoded);
        helper.succeed();
    }

    public static void syncPayloadRoundTrip(GameTestHelper helper) {
        CosmeticsSyncPayload payload = new CosmeticsSyncPayload(42, sampleInventory(helper), false);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            CosmeticsSyncPayload.STREAM_CODEC.encode(buf, payload);
            CosmeticsSyncPayload decoded = CosmeticsSyncPayload.STREAM_CODEC.decode(buf);
            helper.assertValueEqual(decoded.entityId(), 42, "entity id");
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
                ResourceLocation.fromNamespaceAndPath(FemboyApi.MOD_ID, "pride_trans")));
        helper.assertValueEqual(trans.value().stripes().size(), 5, "trans flag stripes");
        helper.assertTrue(registry.size() >= 12, "All built-in patterns should load, got " + registry.size());
        helper.succeed();
    }

    public static void creativeTabContainsItems(GameTestHelper helper) {
        CreativeModeTabs.tryRebuildTabContents(FeatureFlags.DEFAULT_FLAGS, true, helper.getLevel().registryAccess());
        CreativeModeTab tab = FemboyItems.TAB.get();
        helper.assertTrue(CreativeModeTabs.allTabs().contains(tab), "femboymod tab is registered");
        helper.assertTrue(tab.contains(new ItemStack(FemboyItems.CAT_EARS.get())), "Cat Ears are in the femboymod tab");
        long modItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(FemboyApi.MOD_ID)).count();
        long plainInTab = tab.getDisplayItems().stream()
                .filter(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(FemboyApi.MOD_ID))
                .filter(stack -> !stack.has(FemboyComponents.COLORWAY.get())).count();
        helper.assertValueEqual(plainInTab, modItems, "every femboymod item is in the tab once");
        long stripedSocks = tab.getDisplayItems().stream()
                .filter(stack -> stack.is(FemboyItems.PROGRAMMING_SOCKS.get()))
                .filter(stack -> stack.has(FemboyComponents.COLORWAY.get()) && stack.get(FemboyComponents.COLORWAY.get()).stripeCount() == 2)
                .count();
        helper.assertValueEqual(stripedSocks, 3L, "black, blue and red striped socks next to the pink ones");
        helper.succeed();
    }

    private static CosmeticInventory sampleInventory(GameTestHelper helper) {
        Registry<ColorwayPattern> patterns = helper.getLevel().registryAccess().registryOrThrow(ColorwayPattern.REGISTRY_KEY);
        ItemStack ears = new ItemStack(FemboyItems.CAT_EARS.get());
        ears.set(FemboyComponents.COLORWAY.get(), new Colorway(0xFFB6D9,
                Optional.of(patterns.getHolderOrThrow(ResourceKey.create(ColorwayPattern.REGISTRY_KEY,
                        ResourceLocation.fromNamespaceAndPath(FemboyApi.MOD_ID, "pride_bi")))),
                Optional.of(0x123456)));
        return CosmeticInventory.EMPTY.with(HEAD, ears)
                .withArmor(net.minecraft.world.entity.EquipmentSlot.HEAD, dev.eliasnvx.femboymod.api.cosmetic.ArmorVisibility.SHOW)
                .withArmor(net.minecraft.world.entity.EquipmentSlot.FEET, dev.eliasnvx.femboymod.api.cosmetic.ArmorVisibility.HIDE);
    }

    private static void assertSame(GameTestHelper helper, CosmeticInventory expected, CosmeticInventory actual) {
        helper.assertValueEqual(actual.all().keySet(), expected.all().keySet(), "slots");
        helper.assertValueEqual(actual.armor(), expected.armor(), "armor visibility");
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
        helper.assertValueEqual(count, expected, "total " + item + " (inventory + worn)");
    }

    /** Runs the body with a fresh mock player and always removes the player afterwards. */
    private static void withPlayer(GameTestHelper helper, Consumer<ServerPlayer> body) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
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
