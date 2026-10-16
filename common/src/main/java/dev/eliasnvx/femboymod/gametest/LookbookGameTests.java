package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.api.cosmetic.LookbookEntry;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsCatalog;
import dev.eliasnvx.femboymod.profile.ProfileHooks;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;

/** Lookbook data and the first-join book. */
public final class LookbookGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("lookbook_covers_every_cosmetic", LookbookGameTests::lookbookCoversEveryCosmetic),
            new CosmeticGameTests.Entry("lookbook_given_once_on_first_join", LookbookGameTests::lookbookGivenOnceOnFirstJoin));

    private LookbookGameTests() {
    }

    /** Every built-in cosmetic says where to get it (rerun tools/lookbook/make_lookbook.py after new items). */
    public static void lookbookCoversEveryCosmetic(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(LookbookEntry.REGISTRY_KEY);
        List<String> missing = CosmeticsCatalog.items().stream()
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("femboymod"))
                .filter(item -> registry.getOptional(LookbookEntry.keyOf(item)).map(e -> e.sources().isEmpty()).orElse(true))
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                .toList();
        helper.assertTrue(missing.isEmpty(), "cosmetics without a lookbook source: " + missing);
        helper.assertTrue(CosmeticsCatalog.items().size() >= 30, "catalog lists the cosmetics");
        helper.succeed();
    }

    public static void lookbookGivenOnceOnFirstJoin(GameTestHelper helper) {
        WearableGameTests.withPlayer(helper, player -> {
            ProfileHooks.onJoin(player);
            ProfileHooks.onJoin(player);
            Item book = FemboyItems.LOOKBOOK.get();
            helper.assertValueEqual(player.getInventory().countItem(book), 1, "lookbooks after two joins");
        });
    }
}
