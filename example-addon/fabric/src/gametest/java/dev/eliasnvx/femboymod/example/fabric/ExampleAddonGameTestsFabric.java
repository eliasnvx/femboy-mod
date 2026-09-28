package dev.eliasnvx.femboymod.example.fabric;

import dev.eliasnvx.femboymod.example.ExampleAddonGameTests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric glue for the shared {@link ExampleAddonGameTests}; every test runs in Fabric's empty 8x8x8 template. */
public final class ExampleAddonGameTestsFabric implements FabricGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void pinSlotRegistered(GameTestHelper helper) {
        ExampleAddonGameTests.pinSlotRegistered(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void pinIsCosmetic(GameTestHelper helper) {
        ExampleAddonGameTests.pinIsCosmetic(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void candyPatternLoaded(GameTestHelper helper) {
        ExampleAddonGameTests.candyPatternLoaded(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void freshPlayerWearsNothing(GameTestHelper helper) {
        ExampleAddonGameTests.freshPlayerWearsNothing(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void effectTypesRegistered(GameTestHelper helper) {
        ExampleAddonGameTests.effectTypesRegistered(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void friendshipSetBonusLoaded(GameTestHelper helper) {
        ExampleAddonGameTests.friendshipSetBonusLoaded(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void pinIsCharm(GameTestHelper helper) {
        ExampleAddonGameTests.pinIsCharm(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void dripDamageRuleLoaded(GameTestHelper helper) {
        ExampleAddonGameTests.dripDamageRuleLoaded(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void profileFieldAndStylePoints(GameTestHelper helper) {
        ExampleAddonGameTests.profileFieldAndStylePoints(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void allTestsRegistered(GameTestHelper helper) {
        helper.assertValueEqual(ExampleAddonGameTests.ALL.size(), 9, "shared tests wired into the Fabric glue");
        helper.succeed();
    }
}
