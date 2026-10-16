package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.world.item.ItemStack;

/**
 * The client knows its own outfit again after leaving and loading the world (the join sync). On 1.20.1 Fabric it
 * arrived before the player existed and was lost; this guards 26.3 against the same. No screenshots, always runs.
 */
public final class RejoinClientTest implements FabricClientGameTest {

    private static final int SETTLE = 20;

    @Override
    public void runTest(ClientGameTestContext context) {
        TestWorldSave save;
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> CosmeticsManager.set(server.getPlayerList().getPlayers().getFirst(),
                    FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get())));
            context.waitTicks(SETTLE);
            save = world.getWorldSave();
        }
        try (TestSingleplayerContext world = save.open()) {
            world.getConnection().waitForChunksRender();
            context.waitTicks(SETTLE);
            boolean known = context.computeOnClient(mc -> !CosmeticsManager.get(mc.player).items().isEmpty());
            if (!known) {
                throw new AssertionError("the client lost its own outfit when joining again");
            }
        }
    }
}
