package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.api.drip.DripLevel;
import dev.eliasnvx.femboymod.api.event.cosmetic.SetBonusEvent;
import dev.eliasnvx.femboymod.cosmetic.Colorways;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.effect.CosmeticEffectsManager;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Phase 2 logic: stats, set bonuses, Drip Level, dyeing. Registered like {@link CosmeticGameTests}. */
public final class WearableGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("socks_add_and_remove_mining_speed", WearableGameTests::socksAddAndRemoveMiningSpeed),
            new CosmeticGameTests.Entry("full_set_activates_bonus", WearableGameTests::fullSetActivatesBonus),
            new CosmeticGameTests.Entry("vanilla_dye_recipe_colors_cosmetic", WearableGameTests::vanillaDyeRecipeColorsCosmetic),
            new CosmeticGameTests.Entry("cat_ear_hoodie_from_hoodie_and_ears", WearableGameTests::catEarHoodieFromHoodieAndEars));

    private static final Vec3 TEST_AREA_CENTER = new Vec3(1.5, 1.0, 1.5);
    private static final Identifier FULL_SET = Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "full_femboy_mode");

    /** Events seen by the recording listener; the listener is registered once (the bus has no removal). */
    private static final List<Identifier> ACTIVATED_SETS = new ArrayList<>();
    private static boolean listening;

    private WearableGameTests() {
    }

    public static void socksAddAndRemoveMiningSpeed(GameTestHelper helper) {
        withPlayer(helper, player -> {
            AttributeInstance breakSpeed = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
            double before = breakSpeed.getValue();
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
            CosmeticEffectsManager.tick(player);
            helper.assertTrue(breakSpeed.getValue() > before, "socks raise block break speed (" + before + " -> " + breakSpeed.getValue() + ")");
            helper.assertValueEqual(FemboyApi.get().getDripLevel(player).level(), 20, "socks drip");

            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, ItemStack.EMPTY);
            CosmeticEffectsManager.tick(player);
            helper.assertValueEqual(breakSpeed.getValue(), before, "modifier removed with the socks");
        });
    }

    public static void fullSetActivatesBonus(GameTestHelper helper) {
        synchronized (ACTIVATED_SETS) {
            if (!listening) {
                FemboyApi.get().events().addListener(SetBonusEvent.Activate.class, e -> ACTIVATED_SETS.add(e.setId()));
                listening = true;
            }
            ACTIVATED_SETS.clear();
        }
        withPlayer(helper, player -> {
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get()));
            CosmeticsManager.set(player, FemboySlots.OUTFIT_TOP, new ItemStack(FemboyItems.OVERSIZED_HOODIE.get()));
            CosmeticsManager.set(player, FemboySlots.OUTFIT_BOTTOM, new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
            CosmeticEffectsManager.tick(player);
            helper.assertFalse(FemboyApi.get().getActiveSetBonuses(player).contains(FULL_SET), "3 of 4 pieces is not a set");

            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.FISHNET_TIGHTS.get()));
            CosmeticEffectsManager.tick(player);
            helper.assertTrue(FemboyApi.get().getActiveSetBonuses(player).contains(FULL_SET), "fishnet tights count as legwear");
            helper.assertTrue(ACTIVATED_SETS.contains(FULL_SET), "SetBonusEvent.Activate posted");

            // 10 ears + 15 hoodie + 15 skirt + 15 tights + 10 set bonus = 65 -> tier 3 (thresholds 20/40/60/80/95)
            DripLevel drip = FemboyApi.get().getDripLevel(player);
            helper.assertValueEqual(drip, new DripLevel(65, 3), "drip level with the full set");
            helper.assertTrue(CosmeticEffectsManager.runningSources(player).stream()
                    .anyMatch(id -> id.getPath().startsWith("set/femboymod/full_femboy_mode/")), "set effects are running");
        });
    }

    public static void vanillaDyeRecipeColorsCosmetic(GameTestHelper helper) {
        CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(FemboyItems.CAT_EARS.get()), new ItemStack(Items.DYE.pick(DyeColor.LIGHT_BLUE))));
        var recipe = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "a dye recipe matches cat ears + dye");
        ItemStack result = recipe.get().value().assemble(input);
        helper.assertTrue(result.is(FemboyItems.CAT_EARS.get()), "result is cat ears");
        helper.assertTrue(Colorways.effective(result).isPresent(), "dyed ears have a colorway");
        helper.succeed();
    }

    /** Hoodie + cat ears -> cat ear hoodie; the dye is kept and it still counts as the set's hoodie. */
    public static void catEarHoodieFromHoodieAndEars(GameTestHelper helper) {
        ItemStack hoodie = new ItemStack(FemboyItems.OVERSIZED_HOODIE.get());
        hoodie.set(FemboyComponents.COLORWAY.get(), Colorway.solid(0xA8E6CF));
        CraftingInput input = CraftingInput.of(2, 1, List.of(hoodie, new ItemStack(FemboyItems.CAT_EARS.get())));
        var recipe = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "hoodie + cat ears has a recipe");
        ItemStack result = recipe.get().value().assemble(input);
        helper.assertTrue(result.is(FemboyItems.CAT_EAR_HOODIE.get()), "result is the cat ear hoodie");
        helper.assertValueEqual(result.get(FemboyComponents.COLORWAY.get()), Colorway.solid(0xA8E6CF), "colorway kept");
        helper.assertTrue(result.is(FemboyTags.HOODIES), "cat ear hoodie is a hoodie (sleeves, set bonus)");
        withPlayer(helper, player -> {
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get()));
            CosmeticsManager.set(player, FemboySlots.OUTFIT_TOP, result);
            CosmeticsManager.set(player, FemboySlots.OUTFIT_BOTTOM, new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
            CosmeticEffectsManager.tick(player);
            helper.assertTrue(FemboyApi.get().getActiveSetBonuses(player).contains(FULL_SET), "full set with the cat ear hoodie");
        });
    }

    private static void withPlayer(GameTestHelper helper, Consumer<ServerPlayer> body) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(TEST_AREA_CENTER));
        try {
            body.accept(player);
            helper.succeed();
        } finally {
            CosmeticEffectsManager.stop(player);
            player.level().getServer().getPlayerList().remove(player);
        }
    }
}
