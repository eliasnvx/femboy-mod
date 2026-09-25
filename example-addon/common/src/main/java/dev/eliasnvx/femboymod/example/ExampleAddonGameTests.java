package dev.eliasnvx.femboymod.example;

import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.backpack.CharmStats;
import dev.eliasnvx.femboymod.api.colorway.ColorwayPattern;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import dev.eliasnvx.femboymod.api.cosmetic.SetBonus;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/** API regression tests: they only use the public API, exactly like a third-party addon would. */
public final class ExampleAddonGameTests {

    public static final List<Entry> ALL = List.of(
            new Entry("pin_slot_registered", ExampleAddonGameTests::pinSlotRegistered),
            new Entry("pin_is_cosmetic", ExampleAddonGameTests::pinIsCosmetic),
            new Entry("candy_pattern_loaded", ExampleAddonGameTests::candyPatternLoaded),
            new Entry("fresh_player_wears_nothing", ExampleAddonGameTests::freshPlayerWearsNothing),
            new Entry("effect_types_registered", ExampleAddonGameTests::effectTypesRegistered),
            new Entry("friendship_set_bonus_loaded", ExampleAddonGameTests::friendshipSetBonusLoaded),
            new Entry("pin_is_charm", ExampleAddonGameTests::pinIsCharm));

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

    public static void effectTypesRegistered(GameTestHelper helper) {
        helper.assertTrue(FemboyApi.get().cosmeticEffectTypes().get(ExampleEffects.XP_TRICKLE).isPresent(), "xp_trickle effect type");
        helper.assertTrue(FemboyApi.get().cosmeticConditionTypes().get(ExampleEffects.DAYTIME).isPresent(), "daytime condition type");
        helper.succeed();
    }

    public static void friendshipSetBonusLoaded(GameTestHelper helper) {
        SetBonus bonus = helper.getLevel().registryAccess().lookupOrThrow(SetBonus.REGISTRY_KEY)
                .getValue(ResourceKey.create(SetBonus.REGISTRY_KEY, Identifier.fromNamespaceAndPath(ExampleAddon.MOD_ID, "friendship")));
        helper.assertTrue(bonus != null && bonus.pieces().size() == 2, "friendship set bonus loaded with two pieces");
        helper.assertTrue(bonus.effects().getFirst().effect() instanceof ExampleEffects.XpTrickle, "set bonus uses the custom effect");
        helper.assertTrue(bonus.effects().getFirst().when().orElseThrow() instanceof ExampleEffects.Daytime, "set bonus uses the custom condition");
        helper.succeed();
    }

    public static void pinIsCharm(GameTestHelper helper) {
        Item pin = ExampleAddon.friendshipPin.get();
        helper.assertTrue(new ItemStack(pin).is(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(FemboyApi.MOD_ID, "charms"))),
                "pin is in #femboymod:charms");
        helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(CharmStats.REGISTRY_KEY)
                .getValue(CharmStats.keyOf(pin)) != null, "pin has charm stats");
        helper.succeed();
    }
}
