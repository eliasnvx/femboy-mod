package dev.eliasnvx.femboymod.compat.trinkets;

import com.mojang.authlib.GameProfile;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Runs only with Trinkets installed ({@code -Pcompat_slots=true}): our data gives players the slots, items fit, they count. */
public final class TrinketsGameTest {

    private TrinketsGameTest() {
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("trinkets", path));
    }

    public static void cosmeticsInTrinketsSlotsCount(GameTestHelper helper) {
        // not logged in: Cardinal Components disconnects test connections, which can't take its sync packets
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "trinkets-test"));
        ItemStack ears = new ItemStack(FemboyItems.CAT_EARS.get());
        helper.assertTrue(ears.is(tag("head/hat")), "cat ears fit the Trinkets hat slot");
        helper.assertTrue(new ItemStack(FemboyItems.SNEAKERS.get()).is(tag("feet/shoes")), "sneakers fit the Trinkets shoes slot");
        helper.assertFalse(new ItemStack(FemboyItems.CANVAS_BACKPACK.get()).is(tag("chest/back")), "backpacks don't go into Trinkets");
        TrinketInventory hat = TrinketsApi.getTrinketComponent(player).orElseThrow().getInventory()
                .getOrDefault("head", Map.of()).get("hat");
        helper.assertTrue(hat != null && hat.getContainerSize() > 0, "players have the head/hat slot (data/trinkets/entities)");
        hat.setItem(0, ears);
        helper.assertTrue(CosmeticsManager.worn(player).get(FemboySlots.HEAD_ACCESSORY).is(FemboyItems.CAT_EARS.get()),
                "ears in the Trinkets hat slot are worn");
        hat.setItem(0, ItemStack.EMPTY);
        helper.succeed();
    }
}
