package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.combat.DripCombat;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.effect.CosmeticEffectsManager;
import dev.eliasnvx.femboymod.entity.Bug;
import dev.eliasnvx.femboymod.entity.CaffeinatedZombie;
import dev.eliasnvx.femboymod.entity.HissyCat;
import dev.eliasnvx.femboymod.entity.FemboyEntities;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Hostile meme mobs and Drip in combat. */
public final class MobGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("drip_reduces_bug_damage", MobGameTests::dripReducesBugDamage),
            new CosmeticGameTests.Entry("socks_hit_bugs_harder", MobGameTests::socksHitBugsHarder),
            new CosmeticGameTests.Entry("meme_mobs_use_config_and_drip", MobGameTests::memeMobsUseConfigAndDrip));

    private static final BlockPos MOB_POS = new BlockPos(1, 2, 1);
    private static final float HIT = 4.0F;
    private static final float EPSILON = 1.0E-4F;

    private MobGameTests() {
    }

    /** No cosmetics: bugs hit normally. Full set (drip tier 3): 0.6x ({@code drip_damage/bugs.json}). */
    public static void dripReducesBugDamage(GameTestHelper helper) {
        Bug bug = helper.spawnWithNoFreeWill(FemboyEntities.BUG.get(), MOB_POS);
        WearableGameTests.withPlayer(helper, player -> {
            DamageSource bite = player.damageSources().mobAttack(bug);
            helper.assertValueEqual(DripCombat.modifyIncoming(player, helper.getLevel(), bite, HIT), HIT, "no drip, full damage");

            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get()));
            CosmeticsManager.set(player, FemboySlots.OUTFIT_TOP, new ItemStack(FemboyItems.OVERSIZED_HOODIE.get()));
            CosmeticsManager.set(player, FemboySlots.OUTFIT_BOTTOM, new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.FISHNET_TIGHTS.get()));
            CosmeticEffectsManager.tick(player);
            int tier = FemboyMod.api().getDripLevel(player).tier();
            helper.assertValueEqual(tier, 3, "full set is drip tier 3");
            float reduced = DripCombat.modifyIncoming(player, helper.getLevel(), bite, HIT);
            helper.assertTrue(Math.abs(reduced - HIT * 0.6F) < EPSILON, "tier 3 takes 60% from bugs, got " + reduced);
        });
    }

    /** Stats come from config; drip softens the angry memes too ({@code drip_damage/meme_monsters.json}). */
    public static void memeMobsUseConfigAndDrip(GameTestHelper helper) {
        CommonConfig.Mobs mobs = FemboyConfig.common().mobs();
        CaffeinatedZombie zombie = helper.spawnWithNoFreeWill(FemboyEntities.CAFFEINATED_ZOMBIE.get(), MOB_POS);
        HissyCat cat = helper.spawnWithNoFreeWill(FemboyEntities.HISSY_CAT.get(), MOB_POS.east());
        helper.assertValueEqual((double) zombie.getMaxHealth(), mobs.caffeinatedZombie().health(), "zombie health from config");
        helper.assertValueEqual((double) cat.getMaxHealth(), mobs.hissyCat().health(), "cat health from config");
        WearableGameTests.withPlayer(helper, player -> {
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get()));
            CosmeticsManager.set(player, FemboySlots.OUTFIT_TOP, new ItemStack(FemboyItems.OVERSIZED_HOODIE.get()));
            CosmeticsManager.set(player, FemboySlots.OUTFIT_BOTTOM, new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.FISHNET_TIGHTS.get()));
            CosmeticEffectsManager.tick(player);
            float hit = DripCombat.modifyIncoming(player, helper.getLevel(), player.damageSources().mobAttack(zombie), HIT);
            helper.assertTrue(Math.abs(hit - HIT * 0.75F) < EPSILON, "tier 3 takes 75% from meme monsters, got " + hit);
        });
    }

    /** Programming socks: x1.5 against {@code #femboymod:bugs}, unchanged against zombies; gone when taken off. */
    public static void socksHitBugsHarder(GameTestHelper helper) {
        Bug bug = helper.spawnWithNoFreeWill(FemboyEntities.BUG.get(), MOB_POS);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, MOB_POS.east());
        WearableGameTests.withPlayer(helper, player -> {
            DamageSource punch = player.damageSources().playerAttack(player);
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
            CosmeticEffectsManager.tick(player);
            helper.assertTrue(Math.abs(DripCombat.modifyIncoming(bug, helper.getLevel(), punch, HIT) - HIT * 1.5F) < EPSILON,
                    "socks: +50% against bugs");
            helper.assertValueEqual(DripCombat.modifyIncoming(zombie, helper.getLevel(), punch, HIT), HIT, "no bonus against zombies");

            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, ItemStack.EMPTY);
            CosmeticEffectsManager.tick(player);
            helper.assertValueEqual(DripCombat.modifyIncoming(bug, helper.getLevel(), punch, HIT), HIT, "bonus removed with the socks");
        });
    }
}
