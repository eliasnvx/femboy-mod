package dev.eliasnvx.femboymod.example;

import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.colorway.ColorwayPattern;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/** API regression tests: they only use the public API, exactly like a third-party addon would. */
public final class ExampleAddonGameTests {

    public static final List<Entry> ALL = List.of(
            new Entry("pin_slot_registered", ExampleAddonGameTests::pinSlotRegistered),
            new Entry("pin_is_cosmetic", ExampleAddonGameTests::pinIsCosmetic),
            new Entry("candy_pattern_loaded", ExampleAddonGameTests::candyPatternLoaded),
            new Entry("fresh_player_wears_nothing", ExampleAddonGameTests::freshPlayerWearsNothing));

    private ExampleAddonGameTests() {
    }

    public record Entry(String name, Consumer<GameTestHelper> body) {
    }

    public static void pinSlotRegistered(GameTestHelper helper) {
        helper.assertTrue(FemboyApi.get().cosmeticSlots().get(ExampleAddon.PIN_SLOT).isPresent(), "pin slot registered");
        helper.assertTrue(FemboyApi.get().cosmeticSlots().isFrozen(), "slot registry frozen after init");
        helper.succeed();
    }

    public static void pinIsCosmetic(GameTestHelper helper) {
        Cosmetic cosmetic = new ItemStack(ExampleAddon.friendshipPin.get()).get(FemboyApi.get().components().cosmetic().get());
        helper.assertTrue(cosmetic != null && cosmetic.slot().equals(ExampleAddon.PIN_SLOT), "pin is worn in the pin slot");
        helper.succeed();
    }

    public static void candyPatternLoaded(GameTestHelper helper) {
        ColorwayPattern candy = helper.getLevel().registryAccess().lookupOrThrow(ColorwayPattern.REGISTRY_KEY)
                .getValue(ResourceKey.create(ColorwayPattern.REGISTRY_KEY,
                        Identifier.fromNamespaceAndPath(ExampleAddon.MOD_ID, "candy")));
        helper.assertTrue(candy != null && candy.stripes().size() == 4, "candy pattern loaded from the addon's data pack");
        helper.succeed();
    }

    public static void freshPlayerWearsNothing(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            helper.assertTrue(FemboyApi.get().getCosmetics(player).isEmpty(), "fresh player wears nothing");
            helper.succeed();
        } finally {
            player.level().getServer().getPlayerList().remove(player);
        }
    }
}
