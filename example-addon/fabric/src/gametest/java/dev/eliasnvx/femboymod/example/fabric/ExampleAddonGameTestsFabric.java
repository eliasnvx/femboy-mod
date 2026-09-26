package dev.eliasnvx.femboymod.example.fabric;

import dev.eliasnvx.femboymod.example.ExampleAddonGameTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class ExampleAddonGameTestsFabric {

    @GameTest
    public void pinSlotRegistered(GameTestHelper helper) {
        ExampleAddonGameTests.pinSlotRegistered(helper);
    }

    @GameTest
    public void pinIsCosmetic(GameTestHelper helper) {
        ExampleAddonGameTests.pinIsCosmetic(helper);
    }

    @GameTest
    public void candyPatternLoaded(GameTestHelper helper) {
        ExampleAddonGameTests.candyPatternLoaded(helper);
    }

    @GameTest
    public void freshPlayerWearsNothing(GameTestHelper helper) {
        ExampleAddonGameTests.freshPlayerWearsNothing(helper);
    }

    @GameTest
    public void effectTypesRegistered(GameTestHelper helper) {
        ExampleAddonGameTests.effectTypesRegistered(helper);
    }

    @GameTest
    public void friendshipSetBonusLoaded(GameTestHelper helper) {
        ExampleAddonGameTests.friendshipSetBonusLoaded(helper);
    }

    @GameTest
    public void pinIsCharm(GameTestHelper helper) {
        ExampleAddonGameTests.pinIsCharm(helper);
    }

    @GameTest
    public void dripDamageRuleLoaded(GameTestHelper helper) {
        ExampleAddonGameTests.dripDamageRuleLoaded(helper);
    }

    @GameTest
    public void profileFieldAndStylePoints(GameTestHelper helper) {
        ExampleAddonGameTests.profileFieldAndStylePoints(helper);
    }

    @GameTest
    public void allTestsRegistered(GameTestHelper helper) {
        helper.assertValueEqual(ExampleAddonGameTests.ALL.size(), 9, "shared tests wired into the Fabric glue");
        helper.succeed();
    }
}
