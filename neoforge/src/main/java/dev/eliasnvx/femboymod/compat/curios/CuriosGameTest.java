package dev.eliasnvx.femboymod.compat.curios;

import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosSlotTypes;

/** Runs only with Curios installed ({@code -Pcompat_slots=true}): our data gives players the slots, items fit, they count. */
public final class CuriosGameTest {

    private CuriosGameTest() {
    }

    public static void cosmeticsInCuriosSlotsCount(GameTestHelper helper) {
        // not logged in: Curios syncs its slots to joining players, which a test connection can't carry
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "curios-test"), ClientInformation.createDefault());
        ItemStack ears = new ItemStack(FemboyItems.CAT_EARS.get());
        helper.assertTrue(CuriosSlotTypes.getItemSlotTypes(ears, player).containsKey("head"), "cat ears fit the Curios head slot");
        helper.assertTrue(CuriosSlotTypes.getItemSlotTypes(new ItemStack(FemboyItems.SNEAKERS.get()), player).containsKey("feet"),
                "sneakers fit the Curios feet slot");
        helper.assertFalse(CuriosSlotTypes.getItemSlotTypes(new ItemStack(FemboyItems.CANVAS_BACKPACK.get()), player).containsKey("back"),
                "backpacks don't go into Curios");
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        // Curios 17 fills a player's slots when they join a level, which this offline player never does
        inventory.loadDatapacks();
        helper.assertTrue(inventory.getStacksHandler("head").isPresent(), "players have the head slot (data/femboymod/curios/entities)");
        inventory.setEquippedCurio("head", 0, ears);
        helper.assertTrue(CosmeticsManager.worn(player).get(FemboySlots.HEAD_ACCESSORY).is(FemboyItems.CAT_EARS.get()),
                "ears in the Curios head slot are worn");
        inventory.setEquippedCurio("head", 0, ItemStack.EMPTY);
        helper.succeed();
    }
}
