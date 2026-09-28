package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.gametest.BackpackGameTests;
import dev.eliasnvx.femboymod.gametest.CosmeticGameTests;
import dev.eliasnvx.femboymod.gametest.DecorGameTests;
import dev.eliasnvx.femboymod.gametest.GameTestAsserts;
import dev.eliasnvx.femboymod.gametest.MobGameTests;
import dev.eliasnvx.femboymod.gametest.PanelGameTests;
import dev.eliasnvx.femboymod.gametest.ProfileGameTests;
import dev.eliasnvx.femboymod.gametest.WorldGameTests;
import dev.eliasnvx.femboymod.gametest.WearableGameTests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric glue for the shared {@link CosmeticGameTests}. Keep in sync with {@link CosmeticGameTests#ALL}
 * (checked by {@link #allTestsRegistered}). Every test runs in Fabric's empty 8x8x8 template.
 */
public final class FemboyGameTestsFabric implements FabricGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void equipAndUnequip(GameTestHelper helper) {
        CosmeticGameTests.equipAndUnequip(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void rightClickEquipSwaps(GameTestHelper helper) {
        CosmeticGameTests.rightClickEquipSwaps(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void quickMoveConservesItems(GameTestHelper helper) {
        CosmeticGameTests.quickMoveConservesItems(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void deathDropsCosmetics(GameTestHelper helper) {
        CosmeticGameTests.deathDropsCosmetics(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void attachmentCodecRoundTrip(GameTestHelper helper) {
        CosmeticGameTests.attachmentCodecRoundTrip(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void syncPayloadRoundTrip(GameTestHelper helper) {
        CosmeticGameTests.syncPayloadRoundTrip(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void colorwayPatternsLoaded(GameTestHelper helper) {
        CosmeticGameTests.colorwayPatternsLoaded(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void creativeTabContainsItems(GameTestHelper helper) {
        CosmeticGameTests.creativeTabContainsItems(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void socksAddAndRemoveMiningSpeed(GameTestHelper helper) {
        WearableGameTests.socksAddAndRemoveMiningSpeed(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void fullSetActivatesBonus(GameTestHelper helper) {
        WearableGameTests.fullSetActivatesBonus(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void vanillaDyeRecipeColorsCosmetic(GameTestHelper helper) {
        WearableGameTests.vanillaDyeRecipeColorsCosmetic(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void dripReducesBugDamage(GameTestHelper helper) {
        MobGameTests.dripReducesBugDamage(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void memeMobsUseConfigAndDrip(GameTestHelper helper) {
        MobGameTests.memeMobsUseConfigAndDrip(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void fashionCriticJudgesDrip(GameTestHelper helper) {
        MobGameTests.fashionCriticJudgesDrip(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void panelEquipUnequipSwap(GameTestHelper helper) {
        PanelGameTests.equipUnequipSwap(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void panelRejectsAndQuickMove(GameTestHelper helper) {
        PanelGameTests.rejectsAndQuickMove(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void shiftClickHideAndPresets(GameTestHelper helper) {
        PanelGameTests.shiftClickHideAndPresets(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void onlyAFewCuteAnimalsFollow(GameTestHelper helper) {
        MobGameTests.onlyAFewCuteAnimalsFollow(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void socksHitBugsHarder(GameTestHelper helper) {
        MobGameTests.socksHitBugsHarder(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void foxEarsCraftedAndCountAsEars(GameTestHelper helper) {
        WearableGameTests.foxEarsCraftedAndCountAsEars(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void catEarHoodieFromHoodieAndEars(GameTestHelper helper) {
        WearableGameTests.catEarHoodieFromHoodieAndEars(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void rejectsNesting(GameTestHelper helper) {
        BackpackGameTests.rejectsNesting(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void contentsSurviveUpgrades(GameTestHelper helper) {
        BackpackGameTests.contentsSurviveUpgrades(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void menuWritesThrough(GameTestHelper helper) {
        BackpackGameTests.menuWritesThrough(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void droppedWhileOpen(GameTestHelper helper) {
        BackpackGameTests.droppedWhileOpen(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void openSlotIsLocked(GameTestHelper helper) {
        BackpackGameTests.openSlotIsLocked(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void unequipClosesMenu(GameTestHelper helper) {
        BackpackGameTests.unequipClosesMenu(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void charmsWorkWhenWorn(GameTestHelper helper) {
        BackpackGameTests.charmsWorkWhenWorn(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void netheriteIndestructible(GameTestHelper helper) {
        BackpackGameTests.netheriteIndestructible(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void energyDrinkBuffsAndCrash(GameTestHelper helper) {
        BackpackGameTests.energyDrinkBuffsAndCrash(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void energyDrinkJitter(GameTestHelper helper) {
        BackpackGameTests.energyDrinkJitter(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void pinkCreeperHarmless(GameTestHelper helper) {
        WorldGameTests.pinkCreeperHarmless(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void pinkCreeperSpawns(GameTestHelper helper) {
        WorldGameTests.pinkCreeperSpawns(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void rackIsJobSite(GameTestHelper helper) {
        WorldGameTests.rackIsJobSite(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void thrifterTrades(GameTestHelper helper) {
        WorldGameTests.thrifterTrades(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void rackHangAndTake(GameTestHelper helper) {
        WorldGameTests.rackHangAndTake(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void wardrobePresets(GameTestHelper helper) {
        WorldGameTests.wardrobePresets(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void advancementsLoaded(GameTestHelper helper) {
        WorldGameTests.advancementsLoaded(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void gamerChairSeatsAndHeals(GameTestHelper helper) {
        DecorGameTests.gamerChairSeatsAndHeals(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void setupRatingCountsKinds(GameTestHelper helper) {
        DecorGameTests.setupRatingCountsKinds(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void rubberDuckGrantsInsight(GameTestHelper helper) {
        DecorGameTests.rubberDuckGrantsInsight(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void ledStripCyclesColors(GameTestHelper helper) {
        DecorGameTests.ledStripCyclesColors(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void oresGenerateAndDrop(GameTestHelper helper) {
        DecorGameTests.oresGenerateAndDrop(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void postersAndGlitterColorway(GameTestHelper helper) {
        DecorGameTests.postersAndGlitterColorway(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void profileSavesAndKeepsUnknownFields(GameTestHelper helper) {
        ProfileGameTests.profileSavesAndKeepsUnknownFields(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void stylePointsEarnSpendAndEvents(GameTestHelper helper) {
        ProfileGameTests.stylePointsEarnSpendAndEvents(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void collectionUnlocksOnce(GameTestHelper helper) {
        ProfileGameTests.collectionUnlocksOnce(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void gameRulesGateFeatures(GameTestHelper helper) {
        ProfileGameTests.gameRulesGateFeatures(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void armorHidesUnderOutfit(GameTestHelper helper) {
        ProfileGameTests.armorHidesUnderOutfit(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void foodBubbleTeaAndStrawberryMilk(GameTestHelper helper) {
        DecorGameTests.foodBubbleTeaAndStrawberryMilk(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void terminalCraftsAndSaysBtw(GameTestHelper helper) {
        DecorGameTests.terminalCraftsAndSaysBtw(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void v11ClothingSlotsAndStats(GameTestHelper helper) {
        DecorGameTests.v11ClothingSlotsAndStats(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void vibeScannerScoresAndRanks(GameTestHelper helper) {
        DecorGameTests.vibeScannerScoresAndRanks(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void emotesPlayOnServer(GameTestHelper helper) {
        DecorGameTests.emotesPlayOnServer(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void strayCatTamesAndBringsGifts(GameTestHelper helper) {
        DecorGameTests.strayCatTamesAndBringsGifts(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void cosplayerSellsExclusiveColorways(GameTestHelper helper) {
        DecorGameTests.cosplayerSellsExclusiveColorways(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void geodeCrystalsGrowAndNewOresDrop(GameTestHelper helper) {
        DecorGameTests.geodeCrystalsGrowAndNewOresDrop(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void darkShadesImpressTheCritic(GameTestHelper helper) {
        DecorGameTests.darkShadesImpressTheCritic(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void allTestsRegistered(GameTestHelper helper) {
        GameTestAsserts.assertValueEqual(helper, CosmeticGameTests.all().size(), 58, "shared tests wired into the Fabric glue");
        helper.succeed();
    }
}
