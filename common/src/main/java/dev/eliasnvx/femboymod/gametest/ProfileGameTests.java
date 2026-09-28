package dev.eliasnvx.femboymod.gametest;

import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import dev.eliasnvx.femboymod.network.CosmeticsSyncPayload;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import dev.eliasnvx.femboymod.cosmetic.ArmorHiding;
import dev.eliasnvx.femboymod.api.cosmetic.ArmorVisibility;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.api.event.profile.CollectionUnlockEvent;
import dev.eliasnvx.femboymod.api.event.profile.StylePointsEvent;
import dev.eliasnvx.femboymod.api.profile.FemboyProfileFields;
import dev.eliasnvx.femboymod.api.profile.PlayerProfile;
import dev.eliasnvx.femboymod.combat.DripCombat;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsEvents;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.platform.PlatformHelper;
import dev.eliasnvx.femboymod.profile.ProfileData;
import dev.eliasnvx.femboymod.profile.StyleCoupons;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import dev.eliasnvx.femboymod.world.FemboyGameRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

/** Player profile, Style Points, collection and the femboymod game rules. */
public final class ProfileGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("profile_saves_and_keeps_unknown_fields", ProfileGameTests::profileSavesAndKeepsUnknownFields),
            new CosmeticGameTests.Entry("style_points_earn_spend_and_events", ProfileGameTests::stylePointsEarnSpendAndEvents),
            new CosmeticGameTests.Entry("collection_unlocks_once", ProfileGameTests::collectionUnlocksOnce),
            new CosmeticGameTests.Entry("game_rules_gate_features", ProfileGameTests::gameRulesGateFeatures),
            new CosmeticGameTests.Entry("armor_hides_under_outfit", ProfileGameTests::armorHidesUnderOutfit));

    private static final ResourceLocation TEST_REASON = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "gametest");
    private static final ResourceLocation BLOCKED_REASON = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "gametest_blocked");
    private static final List<ResourceLocation> UNLOCKS = new ArrayList<>();
    private static boolean listenersAdded;

    private ProfileGameTests() {
    }

    private static synchronized void addListeners() {
        if (!listenersAdded) {
            listenersAdded = true;
            FemboyMod.api().events().addListener(StylePointsEvent.class, e -> {
                if (e.reason().equals(BLOCKED_REASON)) {
                    e.cancel();
                }
            });
            FemboyMod.api().events().addListener(CollectionUnlockEvent.class, e -> {
                synchronized (UNLOCKS) {
                    UNLOCKS.add(e.item());
                }
            });
        }
    }

    public static void profileSavesAndKeepsUnknownFields(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            PlayerProfile profile = FemboyMod.api().getProfile(player);
            profile.set(FemboyProfileFields.BEST_DRIP_LEVEL, 42);
            ProfileData stored = PlatformHelper.getProfile(player).with("otheraddon:clan", StringTag.valueOf("pink"));
            var encoded = ProfileData.CODEC.encodeStart(NbtOps.INSTANCE, stored).getOrThrow();
            ProfileData decoded = ProfileData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
            PlatformHelper.setProfile(player, decoded);
            helper.assertValueEqual(profile.get(FemboyProfileFields.BEST_DRIP_LEVEL), 42, "value survives saving");
            helper.assertTrue(decoded.values().contains("otheraddon:clan"), "fields of removed addons are kept");
            helper.assertValueEqual(profile.get(FemboyProfileFields.CRITIC_REVIEWS), 0, "unset field reads its default");
        });
    }

    public static void stylePointsEarnSpendAndEvents(GameTestHelper helper) {
        addListeners();
        WearableGameTests.withPlayer(helper, player -> {
            PlayerProfile profile = FemboyMod.api().getProfile(player);
            helper.assertTrue(FemboyMod.api().addStylePoints(player, 30, TEST_REASON), "earn");
            helper.assertValueEqual(profile.get(FemboyProfileFields.STYLE_POINTS), 30, "balance");
            helper.assertValueEqual(profile.get(FemboyProfileFields.STYLE_POINTS_EARNED), 30, "lifetime total");
            helper.assertFalse(FemboyMod.api().addStylePoints(player, 10, BLOCKED_REASON), "listener cancels");
            helper.assertFalse(FemboyMod.api().addStylePoints(player, -40, TEST_REASON), "can't overspend");

            int cost = FemboyConfig.common().stylePoints().couponCost();
            helper.assertTrue(StyleCoupons.redeem(player), "redeem a coupon");
            helper.assertValueEqual(profile.get(FemboyProfileFields.STYLE_POINTS), 30 - cost, "coupon cost paid");
            helper.assertTrue(player.getInventory().contains(new ItemStack(FemboyItems.STYLE_COUPON.get())), "coupon given");
            helper.assertFalse(StyleCoupons.redeem(player), "not enough points for a second one");
            helper.assertValueEqual(profile.get(FemboyProfileFields.STYLE_POINTS_EARNED), 30, "spending keeps the lifetime total");

            withRule(helper, FemboyGameRules.STYLE_POINTS, false, () ->
                    helper.assertFalse(FemboyMod.api().addStylePoints(player, 5, TEST_REASON), "game rule stops earning"));
        });
    }

    public static void collectionUnlocksOnce(GameTestHelper helper) {
        addListeners();
        WearableGameTests.withPlayer(helper, player -> {
            PlayerProfile profile = FemboyMod.api().getProfile(player);
            ResourceLocation ears = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "bear_ears");
            int unlocksBefore = countUnlocks(ears);
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.BEAR_EARS.get()));
            int points = profile.get(FemboyProfileFields.STYLE_POINTS);
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, ItemStack.EMPTY);
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.BEAR_EARS.get()));
            helper.assertTrue(profile.get(FemboyProfileFields.COLLECTION).contains(ears), "bear ears collected");
            helper.assertValueEqual(countUnlocks(ears) - unlocksBefore, 1, "unlock event fired once");
            helper.assertValueEqual(profile.get(FemboyProfileFields.STYLE_POINTS), points, "no points for wearing it again");
            helper.assertValueEqual(profile.get(FemboyProfileFields.EQUIPS), 2, "both equips counted");
        });
    }

    public static void gameRulesGateFeatures(GameTestHelper helper) {
        helper.assertValueEqual(DripCombat.pvpMultiplier(4, 1, 10), 1.3F, "three tiers above: +30%");
        helper.assertValueEqual(DripCombat.pvpMultiplier(0, 5, 30), 0.0F, "never negative");
        WearableGameTests.withPlayer(helper, player -> withRule(helper, FemboyGameRules.KEEP_COSMETICS, true, () -> {
            CosmeticsManager.set(player, FemboySlots.NECK, new ItemStack(FemboyItems.UWU_CHOKER.get()));
            CosmeticsEvents.dropOnDeath(player);
            helper.assertTrue(CosmeticsManager.get(player).get(FemboySlots.NECK).is(FemboyItems.UWU_CHOKER.get()), "choker kept on death");
        }));
    }

    /** Hoodie hides the chestplate (auto), overrides win, hidden cosmetics don't hide armor, the game rule wins over all. */
    public static void armorHidesUnderOutfit(GameTestHelper helper) {
        CosmeticInventory hoodie = CosmeticInventory.EMPTY.with(FemboySlots.OUTFIT_TOP, new ItemStack(FemboyItems.OVERSIZED_HOODIE.get()));
        helper.assertTrue(ArmorHiding.isHidden(hoodie, EquipmentSlot.CHEST, true), "hoodie hides the chestplate");
        helper.assertFalse(ArmorHiding.isHidden(hoodie, EquipmentSlot.HEAD, true), "helmet stays");
        helper.assertFalse(ArmorHiding.isHidden(hoodie.withHidden(FemboySlots.OUTFIT_TOP, true), EquipmentSlot.CHEST, true),
                "a hidden hoodie doesn't hide armor");
        helper.assertFalse(ArmorHiding.isHidden(hoodie.withArmor(EquipmentSlot.CHEST, ArmorVisibility.SHOW), EquipmentSlot.CHEST, true),
                "show overrides auto");
        helper.assertTrue(ArmorHiding.isHidden(CosmeticInventory.EMPTY.withArmor(EquipmentSlot.HEAD, ArmorVisibility.HIDE), EquipmentSlot.HEAD, true),
                "hide works without cosmetics");
        helper.assertFalse(ArmorHiding.isHidden(hoodie, EquipmentSlot.CHEST, false), "game rule off: armor always drawn");
        CosmeticInventory socks = CosmeticInventory.EMPTY.with(FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
        helper.assertValueEqual(ArmorHiding.mask(socks, true), 4 | 8, "socks hide leggings and boots");
        helper.assertTrue(ArmorHiding.visible(new ItemStack(Items.ELYTRA), true).is(Items.ELYTRA), "elytra stays visible");
        helper.assertTrue(ArmorHiding.visible(new ItemStack(Items.IRON_CHESTPLATE), true).isEmpty(), "chestplate hidden");

        WearableGameTests.withPlayer(helper, player -> {
            CosmeticsManager.setArmorVisibility(player, EquipmentSlot.FEET, ArmorVisibility.HIDE);
            helper.assertValueEqual(CosmeticsManager.get(player).armorVisibility(EquipmentSlot.FEET), ArmorVisibility.HIDE, "choice stored");
            withRule(helper, FemboyGameRules.ALLOW_HIDDEN_ARMOR, false, () ->
                    helper.assertFalse(CosmeticsSyncPayload.of(player).armorHidingAllowed(), "sync carries the game rule"));
        });
    }

    private static int countUnlocks(ResourceLocation item) {
        synchronized (UNLOCKS) {
            return (int) UNLOCKS.stream().filter(item::equals).count();
        }
    }

    private static void withRule(GameTestHelper helper, GameRules.Key<GameRules.BooleanValue> key, boolean value, Runnable body) {
        GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(key);
        boolean before = rule.get();
        rule.set(value, helper.getLevel().getServer());
        try {
            body.run();
        } finally {
            rule.set(before, helper.getLevel().getServer());
        }
    }
}
