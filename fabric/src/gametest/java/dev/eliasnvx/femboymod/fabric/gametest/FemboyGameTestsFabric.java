package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.gametest.BackpackGameTests;
import dev.eliasnvx.femboymod.gametest.CosmeticGameTests;
import dev.eliasnvx.femboymod.gametest.MobGameTests;
import dev.eliasnvx.femboymod.gametest.WorldGameTests;
import dev.eliasnvx.femboymod.gametest.WearableGameTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric glue for the shared {@link CosmeticGameTests}. Keep in sync with {@link CosmeticGameTests#ALL}
 * (checked by {@link #allTestsRegistered}).
 */
public final class FemboyGameTestsFabric {

    @GameTest
    public void equipAndUnequip(GameTestHelper helper) {
        CosmeticGameTests.equipAndUnequip(helper);
    }

    @GameTest
    public void rightClickEquipSwaps(GameTestHelper helper) {
        CosmeticGameTests.rightClickEquipSwaps(helper);
    }

    @GameTest
    public void quickMoveConservesItems(GameTestHelper helper) {
        CosmeticGameTests.quickMoveConservesItems(helper);
    }

    @GameTest
    public void deathDropsCosmetics(GameTestHelper helper) {
        CosmeticGameTests.deathDropsCosmetics(helper);
    }

    @GameTest
    public void attachmentCodecRoundTrip(GameTestHelper helper) {
        CosmeticGameTests.attachmentCodecRoundTrip(helper);
    }

    @GameTest
    public void syncPayloadRoundTrip(GameTestHelper helper) {
        CosmeticGameTests.syncPayloadRoundTrip(helper);
    }

    @GameTest
    public void colorwayPatternsLoaded(GameTestHelper helper) {
        CosmeticGameTests.colorwayPatternsLoaded(helper);
    }

    @GameTest
    public void creativeTabContainsItems(GameTestHelper helper) {
        CosmeticGameTests.creativeTabContainsItems(helper);
    }

    @GameTest
    public void socksAddAndRemoveMiningSpeed(GameTestHelper helper) {
        WearableGameTests.socksAddAndRemoveMiningSpeed(helper);
    }

    @GameTest
    public void fullSetActivatesBonus(GameTestHelper helper) {
        WearableGameTests.fullSetActivatesBonus(helper);
    }

    @GameTest
    public void vanillaDyeRecipeColorsCosmetic(GameTestHelper helper) {
        WearableGameTests.vanillaDyeRecipeColorsCosmetic(helper);
    }

    @GameTest
    public void dripReducesBugDamage(GameTestHelper helper) {
        MobGameTests.dripReducesBugDamage(helper);
    }

    @GameTest
    public void socksHitBugsHarder(GameTestHelper helper) {
        MobGameTests.socksHitBugsHarder(helper);
    }

    @GameTest
    public void catEarHoodieFromHoodieAndEars(GameTestHelper helper) {
        WearableGameTests.catEarHoodieFromHoodieAndEars(helper);
    }

    @GameTest
    public void rejectsNesting(GameTestHelper helper) {
        BackpackGameTests.rejectsNesting(helper);
    }

    @GameTest
    public void contentsSurviveUpgrades(GameTestHelper helper) {
        BackpackGameTests.contentsSurviveUpgrades(helper);
    }

    @GameTest
    public void menuWritesThrough(GameTestHelper helper) {
        BackpackGameTests.menuWritesThrough(helper);
    }

    @GameTest
    public void droppedWhileOpen(GameTestHelper helper) {
        BackpackGameTests.droppedWhileOpen(helper);
    }

    @GameTest
    public void openSlotIsLocked(GameTestHelper helper) {
        BackpackGameTests.openSlotIsLocked(helper);
    }

    @GameTest
    public void unequipClosesMenu(GameTestHelper helper) {
        BackpackGameTests.unequipClosesMenu(helper);
    }

    @GameTest
    public void charmsWorkWhenWorn(GameTestHelper helper) {
        BackpackGameTests.charmsWorkWhenWorn(helper);
    }

    @GameTest
    public void netheriteIndestructible(GameTestHelper helper) {
        BackpackGameTests.netheriteIndestructible(helper);
    }

    @GameTest
    public void energyDrinkBuffsAndCrash(GameTestHelper helper) {
        BackpackGameTests.energyDrinkBuffsAndCrash(helper);
    }

    @GameTest
    public void energyDrinkJitter(GameTestHelper helper) {
        BackpackGameTests.energyDrinkJitter(helper);
    }

    @GameTest
    public void pinkCreeperHarmless(GameTestHelper helper) {
        WorldGameTests.pinkCreeperHarmless(helper);
    }

    @GameTest
    public void pinkCreeperSpawns(GameTestHelper helper) {
        WorldGameTests.pinkCreeperSpawns(helper);
    }

    @GameTest
    public void rackIsJobSite(GameTestHelper helper) {
        WorldGameTests.rackIsJobSite(helper);
    }

    @GameTest
    public void thrifterTrades(GameTestHelper helper) {
        WorldGameTests.thrifterTrades(helper);
    }

    @GameTest
    public void rackHangAndTake(GameTestHelper helper) {
        WorldGameTests.rackHangAndTake(helper);
    }

    @GameTest
    public void wardrobePresets(GameTestHelper helper) {
        WorldGameTests.wardrobePresets(helper);
    }

    @GameTest
    public void advancementsLoaded(GameTestHelper helper) {
        WorldGameTests.advancementsLoaded(helper);
    }

    @GameTest
    public void allTestsRegistered(GameTestHelper helper) {
        helper.assertValueEqual(CosmeticGameTests.all().size(), 31, "shared tests wired into the Fabric glue");
        helper.succeed();
    }
}
