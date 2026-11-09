package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.ArmorVisibility;
import dev.eliasnvx.femboymod.api.event.cosmetic.CosmeticEquipEvent;
import dev.eliasnvx.femboymod.api.event.cosmetic.CosmeticUnequipEvent;
import dev.eliasnvx.femboymod.config.ServerSettings;
import dev.eliasnvx.femboymod.cosmetic.PvpCombat;
import dev.eliasnvx.femboymod.menu.CosmeticsMenu;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
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
            backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 64))));
            CosmeticsManager.set(player, FemboySlots.BACK, backpack);
            ItemStack own = CosmeticsSyncPayload.of(player, true).cosmetics().get(FemboySlots.BACK);
            ItemStack seen = CosmeticsSyncPayload.of(player, false).cosmetics().get(FemboySlots.BACK);
            helper.assertTrue(own.has(DataComponents.CONTAINER), "the owner sees what's inside");
            helper.assertTrue(seen.is(FemboyItems.CANVAS_BACKPACK.get()), "others see the backpack");
            helper.assertFalse(seen.has(DataComponents.CONTAINER), "but not what's inside");
            helper.assertTrue(CosmeticsManager.get(player).get(FemboySlots.BACK).has(DataComponents.CONTAINER),
                    "the worn backpack keeps its contents");
            CosmeticsManager.set(player, FemboySlots.BACK, ItemStack.EMPTY);
        });
    }

    /**
     * The creative packet decodes like vanilla's creative slot packet (a stack that fails validation is rejected) and
     * does nothing for spectators or with another menu open.
     */
    public static void creativeCosmeticPacketIsChecked(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            Identifier head = FemboySlots.HEAD_ACCESSORY;
            ItemStack ears = new ItemStack(FemboyItems.CAT_EARS.get());
            helper.assertTrue(CosmeticPanelActions.creativeSet(player, head, ears.copy()), "creative, in the inventory: fine");
            CosmeticsManager.set(player, head, ItemStack.EMPTY);

            ItemStack broken = ears.copy();
            broken.set(DataComponents.MAX_STACK_SIZE, 64);
            broken.set(DataComponents.MAX_DAMAGE, 10); // damageable and stackable at once: vanilla refuses to load it
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            Identifier.STREAM_CODEC.encode(buf, head);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, broken);
            boolean rejected = false;
            try {
                CreativeCosmeticSetPayload.STREAM_CODEC.decode(buf);
            } catch (RuntimeException expected) {
                rejected = true;
            } finally {
                buf.release();
            }
            helper.assertTrue(rejected, "an invalid stack doesn't decode");

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
            ServerPlayer second = helper.makeMockServerPlayerInLevel();
            try {
                helper.assertFalse(CosmeticsSyncPayload.of(first, false).armorRevealed(), "no fight, no reveal");
                PvpCombat.fight(first, second);
                helper.assertTrue(PvpCombat.revealsArmor(first) && PvpCombat.revealsArmor(second), "both fighters");
                helper.assertTrue(CosmeticsSyncPayload.of(first, false).armorRevealed(), "everyone gets told");
                helper.assertTrue(CosmeticsSyncPayload.isDirty(second), "and right away");
            } finally {
                second.level().getServer().getPlayerList().remove(second);
            }
            helper.assertFalse(PvpCombat.revealsArmor(second), "forgotten on logout");
            CosmeticsSyncPayload.flushDirty();
        });
    }

    /** What clients get: set bonuses follow the server's game rule; the payload carries every field. */
    public static void serverSettingsFollowTheServer(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var rule = FemboyGameRules.SET_BONUSES.get();
        boolean before = helper.getLevel().getGameRules().get(rule);
        try {
            helper.getLevel().getGameRules().set(rule, false, server);
            helper.assertFalse(ServerSettings.of(server).setBonuses(), "game rule off: sets off for clients too");
            helper.getLevel().getGameRules().set(rule, true, server);
            ServerSettings settings = new ServerSettings(true, List.of(FemboySlots.TAIL), 42);
            io.netty.buffer.ByteBuf buf = Unpooled.buffer();
            try {
                ServerSettings.STREAM_CODEC.encode(buf, settings);
                helper.assertValueEqual(ServerSettings.STREAM_CODEC.decode(buf), settings, "round trip");
            } finally {
                buf.release();
            }
        } finally {
            helper.getLevel().getGameRules().set(rule, before, server);
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
            helper.assertValueEqual(player.getInventory().countItem(FemboyItems.CAT_EARS.get()), 1, "still in the inventory");
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
                helper.assertValueEqual(player.getInventory().countItem(FemboyItems.CAT_EARS.get()), 1, "into the inventory");
                helper.assertValueEqual(UNEQUIPS.get(player.getUUID()), 1, "one unequip event");
            } finally {
                UNEQUIPS.remove(player.getUUID());
            }
        });
    }
}
