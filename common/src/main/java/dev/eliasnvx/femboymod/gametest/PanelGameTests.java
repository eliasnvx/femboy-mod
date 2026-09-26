package dev.eliasnvx.femboymod.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.menu.CosmeticPanelActions;
import dev.eliasnvx.femboymod.menu.CosmeticsMenu;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Items;

import java.util.List;

/** Inventory cosmetic panel: every click keeps the item count constant (dupe audit). */
public final class PanelGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("panel_equip_unequip_swap", PanelGameTests::equipUnequipSwap),
            new CosmeticGameTests.Entry("panel_rejects_and_quick_move", PanelGameTests::rejectsAndQuickMove),
            new CosmeticGameTests.Entry("shift_click_equips_hide_and_presets", PanelGameTests::shiftClickHideAndPresets));

    private PanelGameTests() {
    }

    public static void equipUnequipSwap(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            var menu = player.inventoryMenu;
            menu.setCarried(socks(0xF5A9B8));
            helper.assertTrue(CosmeticPanelActions.click(player, FemboySlots.LEGS_OVERLAY, false), "equip from the cursor");
            helper.assertTrue(menu.getCarried().isEmpty(), "cursor emptied");
            helper.assertValueEqual(count(player, FemboyItems.PROGRAMMING_SOCKS.get()), 1, "one pair after equip");

            menu.setCarried(socks(0x2A2A33));
            helper.assertTrue(CosmeticPanelActions.click(player, FemboySlots.LEGS_OVERLAY, false), "swap");
            helper.assertValueEqual(CosmeticsManager.get(player).get(FemboySlots.LEGS_OVERLAY)
                    .get(FemboyComponents.COLORWAY.get()), Colorway.solid(0x2A2A33), "black socks now worn");
            helper.assertValueEqual(menu.getCarried().get(FemboyComponents.COLORWAY.get()), Colorway.solid(0xF5A9B8), "pink ones on the cursor");
            helper.assertValueEqual(count(player, FemboyItems.PROGRAMMING_SOCKS.get()), 2, "two pairs after the swap");

            menu.setCarried(ItemStack.EMPTY);
            helper.assertTrue(CosmeticPanelActions.click(player, FemboySlots.LEGS_OVERLAY, false), "take off to the cursor");
            helper.assertTrue(CosmeticsManager.get(player).get(FemboySlots.LEGS_OVERLAY).isEmpty(), "slot emptied");
            helper.assertValueEqual(count(player, FemboyItems.PROGRAMMING_SOCKS.get()), 1, "one pair: the worn one is now on the cursor");
        });
    }

    public static void rejectsAndQuickMove(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            var menu = player.inventoryMenu;
            menu.setCarried(socks(0xFFFFFF));
            helper.assertFalse(CosmeticPanelActions.click(player, FemboySlots.HEAD_ACCESSORY, false), "socks do not go on the head");
            helper.assertTrue(menu.getCarried().is(FemboyItems.PROGRAMMING_SOCKS.get()), "rejected socks stay on the cursor");
            helper.assertTrue(CosmeticsManager.get(player).get(FemboySlots.HEAD_ACCESSORY).isEmpty(), "head slot untouched");
            CosmeticPanelActions.click(player, FemboySlots.LEGS_OVERLAY, false);

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
            }
            helper.assertFalse(CosmeticPanelActions.click(player, FemboySlots.LEGS_OVERLAY, true), "full inventory: shift-click does nothing");
            helper.assertValueEqual(count(player, FemboyItems.PROGRAMMING_SOCKS.get()), 1, "nothing lost with a full inventory");

            player.getInventory().setItem(0, ItemStack.EMPTY);
            helper.assertTrue(CosmeticPanelActions.click(player, FemboySlots.LEGS_OVERLAY, true), "shift-click into the free slot");
            helper.assertTrue(player.getInventory().getItem(0).is(FemboyItems.PROGRAMMING_SOCKS.get()), "socks in the inventory");
            helper.assertValueEqual(count(player, FemboyItems.PROGRAMMING_SOCKS.get()), 1, "exactly one pair after shift-click");

            // Another menu open: the panel does nothing
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> ChestMenu.threeRows(id, inv), Component.empty()));
            player.inventoryMenu.setCarried(socks(0xFFFFFF));
            helper.assertFalse(CosmeticPanelActions.click(player, FemboySlots.LEGS_OVERLAY, false), "ignored while a chest is open");
            player.closeContainer();
        });
    }

    /** Shift-click equips from the inventory; hiding survives a save; presets move items, never copy them. */
    public static void shiftClickHideAndPresets(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().setItem(9, socks(0xF5A9B8));
            player.inventoryMenu.quickMoveStack(player, 9);
            helper.assertTrue(CosmeticsManager.get(player).get(FemboySlots.LEGS_OVERLAY).is(FemboyItems.PROGRAMMING_SOCKS.get()), "shift-click wears the socks");
            helper.assertTrue(player.getInventory().getItem(9).isEmpty(), "and takes them out of the inventory");

            CosmeticsManager.setHidden(player, FemboySlots.LEGS_OVERLAY, true);
            CosmeticInventory saved = CosmeticInventory.CODEC.parse(JsonOps.INSTANCE,
                    CosmeticInventory.CODEC.encodeStart(helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE),
                            CosmeticsManager.get(player)).getOrThrow()).getOrThrow();
            helper.assertTrue(saved.isHidden(FemboySlots.LEGS_OVERLAY), "hidden flag survives a save");
            helper.assertFalse(CosmeticInventory.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow()
                    .isHidden(FemboySlots.LEGS_OVERLAY), "old saves (plain slot map) still load");
            helper.assertFalse(CosmeticPanelActions.creativeSet(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get())),
                    "the creative packet does nothing in survival");

            CosmeticsMenu menu = new CosmeticsMenu(0, player.getInventory());
            menu.clickMenuButton(player, CosmeticsMenu.SAVE_BUTTON);          // preset 1 = the socks
            CosmeticPanelActions.click(player, FemboySlots.LEGS_OVERLAY, true); // take them off into the inventory
            helper.assertTrue(CosmeticsManager.get(player).get(FemboySlots.LEGS_OVERLAY).isEmpty(), "socks taken off");
            menu.clickMenuButton(player, CosmeticsMenu.APPLY_BUTTON);          // put preset 1 back on
            helper.assertTrue(CosmeticsManager.get(player).get(FemboySlots.LEGS_OVERLAY).is(FemboyItems.PROGRAMMING_SOCKS.get()), "preset puts the socks on");
            helper.assertValueEqual(count(player, FemboyItems.PROGRAMMING_SOCKS.get()), 1, "moved from the inventory, not copied");
        });
    }

    private static ItemStack socks(int color) {
        ItemStack stack = new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get());
        stack.set(FemboyComponents.COLORWAY.get(), Colorway.solid(color));
        return stack;
    }

    /** Items of a type in the inventory, on the cursor and worn. */
    private static int count(ServerPlayer player, Item item) {
        int total = player.getInventory().countItem(item);
        if (player.inventoryMenu.getCarried().is(item)) {
            total += player.inventoryMenu.getCarried().getCount();
        }
        for (ItemStack worn : CosmeticsManager.get(player).all().values()) {
            if (worn.is(item)) {
                total += worn.getCount();
            }
        }
        return total;
    }
}
