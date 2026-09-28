package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.backpack.BackpackMenu;
import dev.eliasnvx.femboymod.backpack.BackpackMenuData;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.effect.CosmeticEffectsManager;
import dev.eliasnvx.femboymod.energy.EnergyDrinkItem;
import dev.eliasnvx.femboymod.energy.FemboyEffects;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

/** Phase 3 (SPEC §13): backpack dupe safety, tier upgrades, charms; energy drinks. */
public final class BackpackGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("backpack_rejects_nesting", BackpackGameTests::rejectsNesting),
            new CosmeticGameTests.Entry("backpack_contents_survive_upgrades", BackpackGameTests::contentsSurviveUpgrades),
            new CosmeticGameTests.Entry("backpack_menu_writes_through", BackpackGameTests::menuWritesThrough),
            new CosmeticGameTests.Entry("backpack_dropped_while_open_no_dupe", BackpackGameTests::droppedWhileOpen),
            new CosmeticGameTests.Entry("backpack_open_slot_is_locked", BackpackGameTests::openSlotIsLocked),
            new CosmeticGameTests.Entry("backpack_unequip_closes_menu", BackpackGameTests::unequipClosesMenu),
            new CosmeticGameTests.Entry("backpack_charms_work_when_worn", BackpackGameTests::charmsWorkWhenWorn),
            new CosmeticGameTests.Entry("netherite_backpack_indestructible", BackpackGameTests::netheriteIndestructible),
            new CosmeticGameTests.Entry("energy_drink_buffs_and_crash", BackpackGameTests::energyDrinkBuffsAndCrash),
            new CosmeticGameTests.Entry("energy_drink_jitter", BackpackGameTests::energyDrinkJitter));

    private static final Vec3 TEST_AREA_CENTER = new Vec3(1.5, 1.0, 1.5);
    private static final int HOTBAR_SLOT = 0;

    private BackpackGameTests() {
    }

    private static ItemStack backpackWith(Item tier, ItemStack... contents) {
        ItemStack backpack = new ItemStack(tier);
        backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(contents)));
        return backpack;
    }

    private static int count(ServerPlayer player, Item item, ItemStack backpack) {
        int total = player.getInventory().countItem(item);
        for (ItemStack stack : backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyStream().toList()) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Opens the backpack held in hotbar slot 0 (as right-click would). */
    private static BackpackMenu openFromHand(ServerPlayer player) {
        player.getInventory().selected = HOTBAR_SLOT;
        int rows = player.getInventory().getItem(HOTBAR_SLOT).get(FemboyComponents.BACKPACK.get()).rows();
        return new BackpackMenu(1, player.getInventory(), new BackpackMenuData(false, HOTBAR_SLOT, rows));
    }

    private static int inventoryMenuIndex(BackpackMenu menu, int inventorySlot) {
        int start = menu.rows() * 9 + 3; // bag + charm slots, then 27 main + 9 hotbar
        return inventorySlot < 9 ? start + 27 + inventorySlot : start + inventorySlot - 9;
    }

    public static void rejectsNesting(GameTestHelper helper) {
        withPlayer(helper, player -> {
            helper.assertFalse(BackpackMenu.canStore(new ItemStack(FemboyItems.CANVAS_BACKPACK.get())), "backpack in backpack");
            helper.assertFalse(BackpackMenu.canStore(new ItemStack(Items.SHULKER_BOX)), "shulker box in backpack");
            helper.assertTrue(BackpackMenu.canStore(new ItemStack(Items.STONE)), "stone fits");

            player.getInventory().setItem(HOTBAR_SLOT, new ItemStack(FemboyItems.LEATHER_BACKPACK.get()));
            player.getInventory().setItem(5, new ItemStack(FemboyItems.CANVAS_BACKPACK.get()));
            BackpackMenu menu = openFromHand(player);
            menu.quickMoveStack(player, inventoryMenuIndex(menu, 5));
            helper.assertTrue(player.getInventory().getItem(5).is(FemboyItems.CANVAS_BACKPACK.get()), "shift-click must not nest backpacks");
            helper.assertTrue(player.getInventory().getItem(HOTBAR_SLOT).get(DataComponents.CONTAINER).nonEmptyStream().findAny().isEmpty(),
                    "open backpack stays empty");
        });
    }

    public static void contentsSurviveUpgrades(GameTestHelper helper) {
        ItemStack canvas = backpackWith(FemboyItems.CANVAS_BACKPACK.get(), new ItemStack(Items.DIAMOND, 7));
        canvas.set(FemboyComponents.CHARMS.get(), ItemContainerContents.fromItems(List.of(new ItemStack(FemboyItems.HEART_PIN.get()))));

        CraftingInput leatherInput = CraftingInput.of(2, 1, List.of(canvas, new ItemStack(Items.LEATHER)));
        var leatherRecipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, leatherInput, helper.getLevel());
        helper.assertTrue(leatherRecipe.isPresent(), "canvas + leather recipe");
        ItemStack leather = leatherRecipe.get().value().assemble(leatherInput, helper.getLevel().registryAccess());
        helper.assertTrue(leather.is(FemboyItems.LEATHER_BACKPACK.get()), "leather backpack crafted");

        SmithingRecipeInput smithing = new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), leather, new ItemStack(Items.NETHERITE_INGOT));
        var netheriteRecipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMITHING, smithing, helper.getLevel());
        helper.assertTrue(netheriteRecipe.isPresent(), "netherite smithing recipe");
        ItemStack netherite = netheriteRecipe.get().value().assemble(smithing, helper.getLevel().registryAccess());

        helper.assertTrue(netherite.is(FemboyItems.NETHERITE_BACKPACK.get()), "netherite backpack smithed");
        helper.assertValueEqual(netherite.get(DataComponents.CONTAINER), canvas.get(DataComponents.CONTAINER), "contents kept through both upgrades");
        helper.assertValueEqual(FemboyApi.get().getCharms(netherite).size(), 1, "charms kept");
        helper.assertValueEqual(netherite.get(FemboyComponents.BACKPACK.get()).rows(), 3, "netherite has 27 slots");
        helper.succeed();
    }

    public static void menuWritesThrough(GameTestHelper helper) {
        withPlayer(helper, player -> {
            player.getInventory().setItem(HOTBAR_SLOT, new ItemStack(FemboyItems.CANVAS_BACKPACK.get()));
            player.getInventory().setItem(3, new ItemStack(Items.COBBLESTONE, 40));
            BackpackMenu menu = openFromHand(player);
            ItemStack backpack = player.getInventory().getItem(HOTBAR_SLOT);

            menu.quickMoveStack(player, inventoryMenuIndex(menu, 3));
            helper.assertValueEqual(count(player, Items.COBBLESTONE, backpack), 40, "cobblestone conserved (into backpack)");
            helper.assertTrue(backpack.get(DataComponents.CONTAINER).nonEmptyStream().anyMatch(s -> s.is(Items.COBBLESTONE)),
                    "written straight into the item component");

            menu.quickMoveStack(player, 0);
            helper.assertValueEqual(count(player, Items.COBBLESTONE, backpack), 40, "cobblestone conserved (back out)");
        });
    }

    public static void droppedWhileOpen(GameTestHelper helper) {
        withPlayer(helper, player -> {
            player.getInventory().setItem(HOTBAR_SLOT, new ItemStack(FemboyItems.CANVAS_BACKPACK.get()));
            BackpackMenu menu = openFromHand(player);
            ItemStack backpack = player.getInventory().getItem(HOTBAR_SLOT);

            // the backpack leaves the inventory (dropped / moved away) while the menu is still open
            player.getInventory().setItem(HOTBAR_SLOT, ItemStack.EMPTY);
            helper.assertFalse(menu.stillValid(player), "menu must become invalid once the backpack is gone");

            menu.getSlot(0).set(new ItemStack(Items.DIAMOND, 64));
            helper.assertTrue(backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyStream().findAny().isEmpty(),
                    "no writes into the dropped backpack (would duplicate items)");
            menu.clicked(0, 0, ClickType.PICKUP, player);
            helper.assertTrue(menu.getCarried().isEmpty(), "clicks are ignored on an invalid menu");
        });
    }

    public static void openSlotIsLocked(GameTestHelper helper) {
        withPlayer(helper, player -> {
            player.getInventory().setItem(HOTBAR_SLOT, new ItemStack(FemboyItems.CANVAS_BACKPACK.get()));
            BackpackMenu menu = openFromHand(player);
            int lockedIndex = inventoryMenuIndex(menu, HOTBAR_SLOT);

            menu.clicked(lockedIndex, 0, ClickType.PICKUP, player);
            helper.assertTrue(menu.getCarried().isEmpty(), "cannot pick up the open backpack");
            menu.clicked(lockedIndex, 0, ClickType.THROW, player);
            helper.assertTrue(player.getInventory().getItem(HOTBAR_SLOT).is(FemboyItems.CANVAS_BACKPACK.get()), "cannot throw the open backpack");
            menu.clicked(0, HOTBAR_SLOT, ClickType.SWAP, player);
            helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "number-key swap must not move the backpack into itself");
            helper.assertTrue(player.getInventory().getItem(HOTBAR_SLOT).is(FemboyItems.CANVAS_BACKPACK.get()), "backpack still in the hotbar");
            menu.quickMoveStack(player, lockedIndex);
            helper.assertTrue(player.getInventory().getItem(HOTBAR_SLOT).is(FemboyItems.CANVAS_BACKPACK.get()), "cannot shift-click it either");
        });
    }

    public static void unequipClosesMenu(GameTestHelper helper) {
        withPlayer(helper, player -> {
            CosmeticsManager.set(player, FemboySlots.BACK, new ItemStack(FemboyItems.CANVAS_BACKPACK.get()));
            BackpackMenu menu = new BackpackMenu(1, player.getInventory(), new BackpackMenuData(true, -1, 1));
            helper.assertTrue(menu.stillValid(player), "worn backpack menu valid");
            CosmeticsManager.set(player, FemboySlots.BACK, ItemStack.EMPTY);
            helper.assertFalse(menu.stillValid(player), "taking the backpack off invalidates the menu");
        });
    }

    public static void charmsWorkWhenWorn(GameTestHelper helper) {
        withPlayer(helper, player -> {
            double before = player.getAttribute(Attributes.MAX_HEALTH).getValue();
            ItemStack backpack = new ItemStack(FemboyItems.CANVAS_BACKPACK.get());
            backpack.set(FemboyComponents.CHARMS.get(), ItemContainerContents.fromItems(List.of(new ItemStack(FemboyItems.HEART_PIN.get()))));
            player.getInventory().setItem(4, backpack.copy());
            CosmeticEffectsManager.tick(player);
            helper.assertValueEqual(player.getAttribute(Attributes.MAX_HEALTH).getValue(), before, "charm does nothing in the inventory");

            CosmeticsManager.set(player, FemboySlots.BACK, backpack);
            CosmeticEffectsManager.tick(player);
            helper.assertValueEqual(player.getAttribute(Attributes.MAX_HEALTH).getValue(), before + 2, "heart pin: +1 heart while worn");
        });
    }

    public static void netheriteIndestructible(GameTestHelper helper) {
        var sources = helper.getLevel().damageSources();
        ItemStack netherite = new ItemStack(FemboyItems.NETHERITE_BACKPACK.get());
        ItemStack canvas = new ItemStack(FemboyItems.CANVAS_BACKPACK.get());
        helper.assertFalse(netherite.canBeHurtBy(sources.lava()), "netherite backpack survives lava");
        helper.assertFalse(netherite.canBeHurtBy(sources.inFire()), "and fire");
        // 1.21.1: fire_resistant only covers fire; cactus and explosions go through ItemEntityDamageMixin
        helper.assertTrue(FemboyItems.isDamageResistant(netherite, sources.cactus()), "and cactus");
        helper.assertTrue(FemboyItems.isDamageResistant(netherite, sources.explosion(null, null)), "and explosions");
        helper.assertTrue(canvas.canBeHurtBy(sources.lava()), "canvas burns");
        helper.succeed();
    }

    public static void energyDrinkBuffsAndCrash(GameTestHelper helper) {
        withPlayer(helper, player -> {
            ItemStack can = new ItemStack(FemboyItems.BYTE_ENERGY_PINK.get());
            ((EnergyDrinkItem) can.getItem()).finishUsingItem(can, helper.getLevel(), player);
            // The mock player is in creative (keeps the item), so check the container the drink gives back
            helper.assertTrue(((EnergyDrinkItem) can.getItem()).remainder().is(FemboyItems.EMPTY_ENERGY_CAN.get()), "an empty can is left after drinking");
            helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED), "pink = speed");
            helper.assertTrue(player.hasEffect(FemboyEffects.holder(FemboyEffects.CAFFEINATED)), "caffeine tracked");

            var caffeinated = FemboyEffects.CAFFEINATED.get();
            helper.assertTrue(caffeinated.shouldApplyEffectTickThisTick(1, 0), "crash triggers on the last tick");
            helper.assertFalse(caffeinated.shouldApplyEffectTickThisTick(100, 0), "not before");
            caffeinated.applyEffectTick(player, 0);
            helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "crash: slowness");
            helper.assertTrue(player.hasEffect(MobEffects.DIG_SLOWDOWN), "crash: mining fatigue");
        });
    }

    public static void energyDrinkJitter(GameTestHelper helper) {
        withPlayer(helper, player -> {
            for (int i = 0; i < 3; i++) {
                ItemStack can = new ItemStack(FemboyItems.BYTE_ENERGY_BLUE.get());
                ((EnergyDrinkItem) can.getItem()).finishUsingItem(can, helper.getLevel(), player);
            }
            helper.assertFalse(player.hasEffect(FemboyEffects.holder(FemboyEffects.JITTER)), "3 cans are fine");
            ItemStack fourth = new ItemStack(FemboyItems.BYTE_ENERGY_BLUE.get());
            ((EnergyDrinkItem) fourth.getItem()).finishUsingItem(fourth, helper.getLevel(), player);
            helper.assertTrue(player.hasEffect(FemboyEffects.holder(FemboyEffects.JITTER)), "more than 3 cans in 10 minutes: jitter");
        });
    }

    private static void withPlayer(GameTestHelper helper, Consumer<ServerPlayer> body) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(helper.absoluteVec(TEST_AREA_CENTER));
        try {
            body.accept(player);
            helper.succeed();
        } finally {
            CosmeticEffectsManager.stop(player);
            player.level().getServer().getPlayerList().remove(player);
        }
    }
}
