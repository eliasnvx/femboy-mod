package dev.eliasnvx.femboymod.compat.curios;

import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/** Runs only with Curios installed ({@code -Pcompat_slots=true}): our data gives players the slots, items fit, they count. */
public final class CuriosGameTest {

    private CuriosGameTest() {
    }

    public static void cosmeticsInCuriosSlotsCount(GameTestHelper helper) {
        // not logged in: Curios syncs its slots to joining players, which a test connection can't carry
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "curios-test"));
        {
            ItemStack ears = new ItemStack(FemboyItems.CAT_EARS.get());
            helper.assertTrue(CuriosApi.getItemStackSlots(ears, player).containsKey("head"), "cat ears fit the Curios head slot");
            helper.assertFalse(CuriosApi.getItemStackSlots(new ItemStack(FemboyItems.CANVAS_BACKPACK.get()), player).containsKey("back"),
                    "backpacks don't go into Curios");
            var inventory = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
            helper.assertTrue(inventory.getStacksHandler("head").isPresent(), "players have the head slot (data/femboymod/curios/entities)");
            inventory.setEquippedCurio("head", 0, ears);
            helper.assertTrue(CosmeticsManager.worn(player).get(FemboySlots.HEAD_ACCESSORY).is(FemboyItems.CAT_EARS.get()),
                    "ears in the Curios head slot are worn");
            inventory.setEquippedCurio("head", 0, ItemStack.EMPTY);
            helper.succeed();
        }
    }
}
