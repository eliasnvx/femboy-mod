package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.ArmorVisibility;
import dev.eliasnvx.femboymod.api.event.cosmetic.CosmeticEquipEvent;
import dev.eliasnvx.femboymod.api.event.cosmetic.CosmeticUnequipEvent;
import dev.eliasnvx.femboymod.config.ServerSettings;
import dev.eliasnvx.femboymod.cosmetic.PvpCombat;
import dev.eliasnvx.femboymod.item.ItemList;
import dev.eliasnvx.femboymod.menu.CosmeticsMenu;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.world.FemboyGameRules;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.emote.Emote;
import dev.eliasnvx.femboymod.menu.CosmeticPanelActions;
import dev.eliasnvx.femboymod.network.CosmeticsSyncPayload;
import dev.eliasnvx.femboymod.network.CreativeCosmeticSetPayload;
import dev.eliasnvx.femboymod.network.EmotePayloads;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Multiplayer hardening: what clients can make the server send to others, and what it accepts from them. */
public final class NetworkGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("outfit_changes_sync_once_per_tick", NetworkGameTests::outfitChangesSyncOncePerTick),
            new CosmeticGameTests.Entry("others_see_a_backpack_without_its_contents", NetworkGameTests::othersSeeABackpackWithoutItsContents),
            new CosmeticGameTests.Entry("creative_cosmetic_packet_is_checked", NetworkGameTests::creativeCosmeticPacketIsChecked),
            new CosmeticGameTests.Entry("emotes_play_one_at_a_time", NetworkGameTests::emotesPlayOneAtATime),
            new CosmeticGameTests.Entry("pvp_fights_reveal_hidden_armor", NetworkGameTests::pvpFightsRevealHiddenArmor),
            new CosmeticGameTests.Entry("server_settings_follow_the_server", NetworkGameTests::serverSettingsFollowTheServer),
            new CosmeticGameTests.Entry("presets_respect_equip_rules", NetworkGameTests::presetsRespectEquipRules),
            new CosmeticGameTests.Entry("shift_click_off_posts_unequip", NetworkGameTests::shiftClickOffPostsUnequip));

    /** A count no full stack has: 1.20.1 writes stack counts as a byte, so a client can send up to 127. */
    private static final int OVERSIZED_COUNT = 100;

    /** Players the listeners below act on (the listeners stay registered; they ignore everyone else). */
    private static final Set<UUID> REFUSE_HEAD = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> UNEQUIPS = new ConcurrentHashMap<>();
    private static boolean listening;

    private static synchronized void listen() {
        if (listening) {
            return;
        }
        listening = true;
        FemboyMod.api().events().addListener(CosmeticEquipEvent.class, event -> {
            if (REFUSE_HEAD.contains(event.entity().getUUID()) && FemboySlots.HEAD_ACCESSORY.equals(event.slot())) {
                event.cancel();
            }
        });
        FemboyMod.api().events().addListener(CosmeticUnequipEvent.class, event ->
                UNEQUIPS.computeIfPresent(event.entity().getUUID(), (id, count) -> count + 1));
    }

    private NetworkGameTests() {
    }

    /** Toggles, armor visibility and equips in one tick queue one sync, sent at the end of the tick. */
    public static void outfitChangesSyncOncePerTick(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get()));
            for (int i = 0; i < 5; i++) {
                CosmeticsManager.setHidden(player, FemboySlots.HEAD_ACCESSORY, i % 2 == 0);
            }
            CosmeticsManager.setArmorVisibility(player, EquipmentSlot.HEAD, ArmorVisibility.SHOW);
            helper.assertTrue(CosmeticsSyncPayload.isDirty(player), "queued, not sent right away");
            helper.assertTrue(CosmeticsSyncPayload.flushDirty() >= 1, "sent at the end of the tick");
            helper.assertFalse(CosmeticsSyncPayload.isDirty(player), "once");
        });
    }

    /** The owner gets the backpack with its contents; everyone else only sees the backpack. */
    public static void othersSeeABackpackWithoutItsContents(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            ItemStack backpack = new ItemStack(FemboyItems.CANVAS_BACKPACK.get());
            FemboyComponents.CONTENTS.set(backpack, ItemList.fromItems(List.of(new ItemStack(Items.DIAMOND, 64))));
            CosmeticsManager.set(player, FemboySlots.BACK, backpack);
            ItemStack own = CosmeticsSyncPayload.of(player, true).cosmetics().get(FemboySlots.BACK);
            ItemStack seen = CosmeticsSyncPayload.of(player, false).cosmetics().get(FemboySlots.BACK);
            // backpacks default to empty contents (1.21.1's default component), so this asks what's inside, not has()
            helper.assertFalse(packed(own).isEmpty(), "the owner sees what's inside");
            helper.assertTrue(seen.is(FemboyItems.CANVAS_BACKPACK.get()), "others see the backpack");
            helper.assertTrue(packed(seen).isEmpty(), "but not what's inside");
            helper.assertFalse(packed(CosmeticsManager.get(player).get(FemboySlots.BACK)).isEmpty(),
                    "the worn backpack keeps its contents");
            CosmeticsManager.set(player, FemboySlots.BACK, ItemStack.EMPTY);
        });
    }

    private static List<ItemStack> packed(ItemStack backpack) {
        ItemList contents = FemboyComponents.CONTENTS.get(backpack);
        return contents == null ? List.of() : contents.nonEmptyItems();
    }

    /**
     * The creative packet is checked like vanilla 1.20.1's creative slot packet, which reads stacks as they come (no
     * validated codec yet): more than a full stack or a negative damage is refused. Nothing for spectators or with
     * another menu open either.
     */
    public static void creativeCosmeticPacketIsChecked(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            player.setGameMode(GameType.CREATIVE);
            ResourceLocation head = FemboySlots.HEAD_ACCESSORY;
            ItemStack ears = new ItemStack(FemboyItems.CAT_EARS.get());
            helper.assertTrue(CosmeticPanelActions.creativeSet(player, head, ears.copy()), "creative, in the inventory: fine");
            CosmeticsManager.set(player, head, ItemStack.EMPTY);

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            try {
                new CreativeCosmeticSetPayload(head, ears.copyWithCount(OVERSIZED_COUNT)).write(buf);
                CreativeCosmeticSetPayload oversized = CreativeCosmeticSetPayload.read(buf);
                GameTestAsserts.assertValueEqual(helper, oversized.stack().getCount(), OVERSIZED_COUNT, "the count arrives as sent");
                helper.assertFalse(CosmeticPanelActions.creativeSet(player, oversized.slot(), oversized.stack()),
                        "more than a full stack: refused");
            } finally {
                buf.release();
            }
            ItemStack negative = ears.copy();
            negative.getOrCreateTag().putInt(ItemStack.TAG_DAMAGE, -1);
            helper.assertFalse(CosmeticPanelActions.creativeSet(player, head, negative), "negative damage: refused");

            player.setGameMode(GameType.SPECTATOR);
            helper.assertFalse(CosmeticPanelActions.creativeSet(player, head, ears.copy()), "not for spectators");
            player.setGameMode(GameType.CREATIVE);
            player.containerMenu = new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, 1,
                    player.getInventory(), new net.minecraft.world.SimpleContainer(9), 1);
            helper.assertFalse(CosmeticPanelActions.creativeSet(player, head, ears.copy()), "not with a chest open");
            player.containerMenu = player.inventoryMenu;
            helper.assertTrue(CosmeticsManager.get(player).get(head).isEmpty(), "nothing got through");
        });
    }

    /** A client spamming the emote packet gets one emote at a time (each goes out to everyone around). */
    public static void emotesPlayOneAtATime(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            helper.assertTrue(EmotePayloads.tryPlay(player, Emote.WAVE), "the first one plays");
            helper.assertFalse(EmotePayloads.tryPlay(player, Emote.WAVE), "the same tick: still waving");
            helper.assertFalse(EmotePayloads.tryPlay(player, Emote.HEART_HANDS), "another one waits too");
            EmotePayloads.forget(player);
            helper.assertTrue(EmotePayloads.tryPlay(player, Emote.HEART_HANDS), "after it ends (or a relog) it plays");
            EmotePayloads.forget(player);
        });
    }

    /** A player hitting another reveals both players' hidden armor (sent with the outfit), and only then. */
    public static void pvpFightsRevealHiddenArmor(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, first -> {
            ServerPlayer second = MockPlayers.create(helper); // with a channel, so Forge's login hooks can send to it
            try {
                helper.assertFalse(CosmeticsSyncPayload.of(first, false).armorRevealed(), "no fight, no reveal");
                PvpCombat.fight(first, second);
                helper.assertTrue(PvpCombat.revealsArmor(first) && PvpCombat.revealsArmor(second), "both fighters");
                helper.assertTrue(CosmeticsSyncPayload.of(first, false).armorRevealed(), "everyone gets told");
                helper.assertTrue(CosmeticsSyncPayload.isDirty(second), "and right away");
            } finally {
                second.getServer().getPlayerList().remove(second);
            }
            helper.assertFalse(PvpCombat.revealsArmor(second), "forgotten on logout");
            CosmeticsSyncPayload.flushDirty();
        });
    }

    /** What clients get: set bonuses follow the server's game rule; the payload carries every field. */
    public static void serverSettingsFollowTheServer(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var rule = helper.getLevel().getGameRules().getRule(FemboyGameRules.SET_BONUSES);
        boolean before = rule.get();
        try {
            rule.set(false, server);
            helper.assertFalse(ServerSettings.of(server).setBonuses(), "game rule off: sets off for clients too");
            rule.set(true, server);
            ServerSettings settings = new ServerSettings(true, List.of(FemboySlots.TAIL), 42);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            try {
                settings.write(buf);
                GameTestAsserts.assertValueEqual(helper, ServerSettings.read(buf), settings, "round trip");
            } finally {
                buf.release();
            }
        } finally {
            rule.set(before, server);
        }
        helper.succeed();
    }

    /** A Wardrobe preset can't put on what the player couldn't put on by hand (here: an addon refuses the head slot). */
    public static void presetsRespectEquipRules(GameTestHelper helper) {
        listen();
        WearableGameTests.withPlayer(helper, player -> {
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get()));
            CosmeticsMenu menu = new CosmeticsMenu(0, player.getInventory());
            menu.clickMenuButton(player, CosmeticsMenu.SAVE_BUTTON); // preset 1: the ears
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, ItemStack.EMPTY);
            player.getInventory().add(new ItemStack(FemboyItems.CAT_EARS.get()));
            REFUSE_HEAD.add(player.getUUID());
            try {
                menu.clickMenuButton(player, CosmeticsMenu.APPLY_BUTTON);
            } finally {
                REFUSE_HEAD.remove(player.getUUID());
            }
            helper.assertTrue(CosmeticsManager.get(player).get(FemboySlots.HEAD_ACCESSORY).isEmpty(), "refused: not worn");
            GameTestAsserts.assertValueEqual(helper, player.getInventory().countItem(FemboyItems.CAT_EARS.get()), 1, "still in the inventory");
        });
    }

    /** Shift-clicking a cosmetic off in the outfit menu tells addons it came off (it used to be emptied first). */
    public static void shiftClickOffPostsUnequip(GameTestHelper helper) {
        listen();
        WearableGameTests.withPlayer(helper, player -> {
            UNEQUIPS.put(player.getUUID(), 0);
            try {
                CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get()));
                CosmeticsMenu menu = new CosmeticsMenu(0, player.getInventory());
                int index = CosmeticsManager.orderedSlots().indexOf(FemboySlots.HEAD_ACCESSORY);
                menu.quickMoveStack(player, index);
                helper.assertTrue(CosmeticsManager.get(player).get(FemboySlots.HEAD_ACCESSORY).isEmpty(), "taken off");
                GameTestAsserts.assertValueEqual(helper, player.getInventory().countItem(FemboyItems.CAT_EARS.get()), 1, "into the inventory");
                GameTestAsserts.assertValueEqual(helper, UNEQUIPS.get(player.getUUID()), 1, "one unequip event");
            } finally {
                UNEQUIPS.remove(player.getUUID());
            }
        });
    }
}
