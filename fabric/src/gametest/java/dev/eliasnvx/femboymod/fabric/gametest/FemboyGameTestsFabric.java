package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.gametest.BackpackGameTests;
import dev.eliasnvx.femboymod.gametest.CosmeticGameTests;
import dev.eliasnvx.femboymod.gametest.DecorGameTests;
import dev.eliasnvx.femboymod.gametest.MobGameTests;
import dev.eliasnvx.femboymod.gametest.PanelGameTests;
import dev.eliasnvx.femboymod.gametest.ProfileGameTests;
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
    public void memeMobsUseConfigAndDrip(GameTestHelper helper) {
        MobGameTests.memeMobsUseConfigAndDrip(helper);
    }

    @GameTest
    public void fashionCriticJudgesDrip(GameTestHelper helper) {
        MobGameTests.fashionCriticJudgesDrip(helper);
    }

    @GameTest
    public void panelEquipUnequipSwap(GameTestHelper helper) {
        PanelGameTests.equipUnequipSwap(helper);
    }

    @GameTest
    public void panelRejectsAndQuickMove(GameTestHelper helper) {
        PanelGameTests.rejectsAndQuickMove(helper);
    }

    @GameTest
    public void shiftClickHideAndPresets(GameTestHelper helper) {
        PanelGameTests.shiftClickHideAndPresets(helper);
    }

    @GameTest
    public void onlyAFewCuteAnimalsFollow(GameTestHelper helper) {
        MobGameTests.onlyAFewCuteAnimalsFollow(helper);
    }

    @GameTest
    public void socksHitBugsHarder(GameTestHelper helper) {
        MobGameTests.socksHitBugsHarder(helper);
    }

    @GameTest
    public void foxEarsCraftedAndCountAsEars(GameTestHelper helper) {
        WearableGameTests.foxEarsCraftedAndCountAsEars(helper);
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
    public void gamerChairSeatsAndHeals(GameTestHelper helper) {
        DecorGameTests.gamerChairSeatsAndHeals(helper);
    }

    @GameTest
    public void setupRatingCountsKinds(GameTestHelper helper) {
        DecorGameTests.setupRatingCountsKinds(helper);
    }

    @GameTest
    public void rubberDuckGrantsInsight(GameTestHelper helper) {
        DecorGameTests.rubberDuckGrantsInsight(helper);
    }

    @GameTest
    public void ledStripCyclesColors(GameTestHelper helper) {
        DecorGameTests.ledStripCyclesColors(helper);
    }

    @GameTest
    public void oresGenerateAndDrop(GameTestHelper helper) {
        DecorGameTests.oresGenerateAndDrop(helper);
    }

    @GameTest
    public void postersAndGlitterColorway(GameTestHelper helper) {
        DecorGameTests.postersAndGlitterColorway(helper);
    }

    @GameTest
    public void profileSavesAndKeepsUnknownFields(GameTestHelper helper) {
        ProfileGameTests.profileSavesAndKeepsUnknownFields(helper);
    }

    @GameTest
    public void stylePointsEarnSpendAndEvents(GameTestHelper helper) {
        ProfileGameTests.stylePointsEarnSpendAndEvents(helper);
    }

    @GameTest
    public void collectionUnlocksOnce(GameTestHelper helper) {
        ProfileGameTests.collectionUnlocksOnce(helper);
    }

    @GameTest
    public void gameRulesGateFeatures(GameTestHelper helper) {
        ProfileGameTests.gameRulesGateFeatures(helper);
    }

    @GameTest
    public void armorHidesUnderOutfit(GameTestHelper helper) {
        ProfileGameTests.armorHidesUnderOutfit(helper);
    }

    @GameTest
    public void foodBubbleTeaAndStrawberryMilk(GameTestHelper helper) {
        DecorGameTests.foodBubbleTeaAndStrawberryMilk(helper);
    }

    @GameTest
    public void terminalCraftsAndSaysBtw(GameTestHelper helper) {
        DecorGameTests.terminalCraftsAndSaysBtw(helper);
    }

    @GameTest
    public void v11ClothingSlotsAndStats(GameTestHelper helper) {
        DecorGameTests.v11ClothingSlotsAndStats(helper);
    }

    @GameTest
    public void vibeScannerScoresAndRanks(GameTestHelper helper) {
        DecorGameTests.vibeScannerScoresAndRanks(helper);
    }

    @GameTest
    public void emotesPlayOnServer(GameTestHelper helper) {
        DecorGameTests.emotesPlayOnServer(helper);
    }

    @GameTest
    public void strayCatTamesAndBringsGifts(GameTestHelper helper) {
        DecorGameTests.strayCatTamesAndBringsGifts(helper);
    }

    @GameTest
    public void cosplayerSellsExclusiveColorways(GameTestHelper helper) {
        DecorGameTests.cosplayerSellsExclusiveColorways(helper);
    }

    @GameTest
    public void geodeCrystalsGrowAndNewOresDrop(GameTestHelper helper) {
        DecorGameTests.geodeCrystalsGrowAndNewOresDrop(helper);
    }

    @GameTest
    public void darkShadesImpressTheCritic(GameTestHelper helper) {
        DecorGameTests.darkShadesImpressTheCritic(helper);
    }

    @GameTest
    public void allTestsRegistered(GameTestHelper helper) {
        helper.assertValueEqual(CosmeticGameTests.all().size(), 58, "shared tests wired into the Fabric glue");
        helper.succeed();
    }
}
