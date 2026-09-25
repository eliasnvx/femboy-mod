package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.gametest.CosmeticGameTests;
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
    public void allTestsRegistered(GameTestHelper helper) {
        helper.assertValueEqual(CosmeticGameTests.all().size(), 11, "shared tests wired into the Fabric glue");
        helper.succeed();
    }
}
