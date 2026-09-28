package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.block.ClothingRackBlockEntity;
import dev.eliasnvx.femboymod.block.FemboyBlocks;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.effect.CosmeticEffectsManager;
import dev.eliasnvx.femboymod.entity.FemboyEntities;
import dev.eliasnvx.femboymod.entity.PinkCreeper;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import dev.eliasnvx.femboymod.wardrobe.Outfits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

/** Phase 4 (SPEC §16): Pink Creeper, Thrifter + rack, wardrobe presets, advancements. */
public final class WorldGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("pink_creeper_confetti_is_harmless", WorldGameTests::pinkCreeperHarmless),
            new CosmeticGameTests.Entry("pink_creeper_spawns_in_flower_biomes", WorldGameTests::pinkCreeperSpawns),
            new CosmeticGameTests.Entry("clothing_rack_is_thrifter_job_site", WorldGameTests::rackIsJobSite),
            new CosmeticGameTests.Entry("thrifter_trades_generate", WorldGameTests::thrifterTrades),
            new CosmeticGameTests.Entry("clothing_rack_hang_and_take", WorldGameTests::rackHangAndTake),
            new CosmeticGameTests.Entry("wardrobe_preset_switch", WorldGameTests::wardrobePresets),
            new CosmeticGameTests.Entry("advancements_loaded", WorldGameTests::advancementsLoaded));

    private static final Vec3 TEST_AREA_CENTER = new Vec3(1.5, 1.0, 1.5);

    private WorldGameTests() {
    }

    public static void pinkCreeperHarmless(GameTestHelper helper) {
        withPlayer(helper, player -> {
            BlockPos floor = new BlockPos(1, 0, 1);
            helper.setBlock(floor.east(), Blocks.STONE);
            helper.setBlock(floor.west(), Blocks.GLASS);
            PinkCreeper creeper = helper.spawn(FemboyEntities.PINK_CREEPER.get(), new BlockPos(1, 1, 2));
            float health = player.getHealth();
            creeper.confettiBurst(helper.getLevel());

            helper.assertBlockPresent(Blocks.STONE, floor.east());
            helper.assertBlockPresent(Blocks.GLASS, floor.west());
            helper.assertTrue(creeper.isRemoved(), "the creeper is gone after bursting");
            helper.assertValueEqual(player.getHealth(), health, "players take no damage by default");
            helper.assertTrue(player.getDeltaMovement().lengthSqr() > 0, "but get a playful push");
        });
    }

    public static void pinkCreeperSpawns(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        helper.assertTrue(spawnsIn(biomes.getOrThrow(Biomes.FLOWER_FOREST)), "spawns in flower forests");
        helper.assertTrue(spawnsIn(biomes.getOrThrow(Biomes.CHERRY_GROVE)), "spawns in cherry groves");
        helper.assertFalse(spawnsIn(biomes.getOrThrow(Biomes.PLAINS)), "not in plain plains");
        helper.succeed();
    }

    private static boolean spawnsIn(Biome biome) {
        MobSpawnSettings spawns = biome.getMobSettings();
        return spawns.getMobs(MobCategory.MONSTER).unwrap().stream()
                .anyMatch(data -> data.type == FemboyEntities.PINK_CREEPER.get());
    }

    public static void rackIsJobSite(GameTestHelper helper) {
        var poi = PoiTypes.forState(FemboyBlocks.CLOTHING_RACK.get().defaultBlockState());
        helper.assertTrue(poi.isPresent() && poi.get().is(FemboyBlocks.THRIFTER_POI), "clothing rack is the thrifter POI");
        helper.succeed();
    }

    public static void thrifterTrades(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1));
        villager.setVillagerData(villager.getVillagerData().setProfession(FemboyBlocks.THRIFTER.get()).setLevel(1));
        List<MerchantOffer> offers = villager.getOffers();
        helper.assertTrue(!offers.isEmpty(), "thrifter has trades");
        helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().getItem().builtInRegistryHolder().key().location().getNamespace().equals(FemboyMod.MOD_ID)
                || o.getCostA().is(Items.STRING)), "trades come from the femboymod trade set");
        villager.discard();
        helper.succeed();
    }

    public static void rackHangAndTake(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, FemboyBlocks.CLOTHING_RACK.get());
        ClothingRackBlockEntity rack = helper.getBlockEntity(pos);
        ItemStack hand = new ItemStack(FemboyItems.PLEATED_SKIRT.get(), 1);
        helper.assertTrue(rack.hang(hand), "hangs");
        helper.assertTrue(hand.isEmpty(), "moved from the hand (no copy)");
        for (int i = 0; i < ClothingRackBlockEntity.SLOTS - 1; i++) {
            rack.hang(new ItemStack(Items.LEATHER));
        }
        helper.assertFalse(rack.hang(new ItemStack(Items.LEATHER)), "only 3 hooks");
        helper.assertTrue(rack.takeLast().is(Items.LEATHER), "takes the last hung item");
        helper.succeed();
    }

    public static void wardrobePresets(GameTestHelper helper) {
        withPlayer(helper, player -> {
            SimpleContainer wardrobe = new SimpleContainer(27);
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get()));
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
            Outfits.save(player, 0);

            // switch to a different outfit: ears off (into the wardrobe), fishnet tights on (from the inventory)
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, ItemStack.EMPTY);
            wardrobe.setItem(0, new ItemStack(FemboyItems.CAT_EARS.get()));
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.FISHNET_TIGHTS.get()));
            player.getInventory().setItem(5, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));

            int changed = Outfits.apply(player, wardrobe, 0);
            helper.assertValueEqual(changed, 2, "two slots switched");
            helper.assertTrue(CosmeticsManager.get(player).get(FemboySlots.HEAD_ACCESSORY).is(FemboyItems.CAT_EARS.get()), "ears from the wardrobe");
            helper.assertTrue(CosmeticsManager.get(player).get(FemboySlots.LEGS_OVERLAY).is(FemboyItems.PROGRAMMING_SOCKS.get()), "socks from the inventory");
            helper.assertTrue(wardrobe.getItem(0).isEmpty() || !wardrobe.getItem(0).is(FemboyItems.CAT_EARS.get()), "ears left the wardrobe");
            helper.assertTrue(wardrobe.countItem(FemboyItems.FISHNET_TIGHTS.get()) == 1, "tights were put into the wardrobe");
            helper.assertValueEqual(player.getInventory().countItem(FemboyItems.PROGRAMMING_SOCKS.get()), 0, "socks left the inventory (moved, not copied)");
            helper.assertValueEqual(Outfits.apply(player, wardrobe, 3), 0, "empty preset does nothing");
        });
    }

    public static void advancementsLoaded(GameTestHelper helper) {
        var advancements = helper.getLevel().getServer().getAdvancements();
        for (String name : List.of("root", "programming_socks", "drip_max", "caffeine_overflow", "thrifted", "confetti_survivor", "full_femboy_mode",
                "battlestation", "just_need_a_break", "cant_decide", "ready_to_deploy", "rubber_duck_debugging", "btw", "immaculate_vibes")) {
            helper.assertTrue(advancements.get(new ResourceLocation(FemboyMod.MOD_ID, "main/" + name)) != null, "advancement " + name);
        }
        helper.succeed();
    }

    private static void withPlayer(GameTestHelper helper, Consumer<ServerPlayer> body) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(helper.absoluteVec(TEST_AREA_CENTER));
        try {
            body.accept(player);
            helper.succeed();
        } finally {
            CosmeticEffectsManager.stop(player);
            player.level().getServer().getPlayerList().remove(player);
        }
    }
}
