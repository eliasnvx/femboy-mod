package dev.eliasnvx.femboymod.gametest;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.combat.DripCombat;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.effect.BuiltinEffects;
import dev.eliasnvx.femboymod.effect.CosmeticEffectsManager;
import dev.eliasnvx.femboymod.entity.Bug;
import dev.eliasnvx.femboymod.entity.CaffeinatedZombie;
import dev.eliasnvx.femboymod.entity.FashionCritic;
import dev.eliasnvx.femboymod.entity.HissyCat;
import dev.eliasnvx.femboymod.entity.FemboyEntities;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Hostile meme mobs and Drip in combat. */
public final class MobGameTests {

    public static final List<CosmeticGameTests.Entry> ALL = List.of(
            new CosmeticGameTests.Entry("drip_reduces_bug_damage", MobGameTests::dripReducesBugDamage),
            new CosmeticGameTests.Entry("socks_hit_bugs_harder", MobGameTests::socksHitBugsHarder),
            new CosmeticGameTests.Entry("meme_mobs_use_config_and_drip", MobGameTests::memeMobsUseConfigAndDrip),
            new CosmeticGameTests.Entry("fashion_critic_judges_drip", MobGameTests::fashionCriticJudgesDrip),
            new CosmeticGameTests.Entry("only_a_few_cute_animals_follow", MobGameTests::onlyAFewCuteAnimalsFollow));

    private static final BlockPos MOB_POS = new BlockPos(1, 2, 1);
    private static final float HIT = 4.0F;
    private static final float EPSILON = 1.0E-4F;
    private static final int CATS = 5;

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

    /** Low Drip: the critic hits x1.6 and its review slows you; tier 3: it hits softer and is weakened itself. */
    public static void fashionCriticJudgesDrip(GameTestHelper helper) {
        FashionCritic critic = helper.spawnWithNoFreeWill(FemboyEntities.FASHION_CRITIC.get(), MOB_POS);
        WearableGameTests.withPlayer(helper, player -> {
            DamageSource slap = player.damageSources().mobAttack(critic);
            float hit = DripCombat.modifyIncoming(player, helper.getLevel(), slap, HIT);
            helper.assertTrue(Math.abs(hit - HIT * 1.6F) < EPSILON, "no drip: the critic hits x1.6, got " + hit);
            critic.review(player);
            helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "bad review slows the player");
            helper.assertFalse(critic.hasEffect(MobEffects.WEAKNESS), "the critic is not impressed");

            player.removeAllEffects();
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get()));
            CosmeticsManager.set(player, FemboySlots.OUTFIT_TOP, new ItemStack(FemboyItems.OVERSIZED_HOODIE.get()));
            CosmeticsManager.set(player, FemboySlots.OUTFIT_BOTTOM, new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
            CosmeticsManager.set(player, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.FISHNET_TIGHTS.get()));
            CosmeticEffectsManager.tick(player);
            critic.review(player);
            helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "good review does not slow the player");
            helper.assertTrue(critic.hasEffect(MobEffects.WEAKNESS), "impressed critic is weakened");
            float softer = DripCombat.modifyIncoming(player, helper.getLevel(), slap, HIT);
            helper.assertTrue(Math.abs(softer - HIT * 0.9F) < EPSILON, "tier 3: the critic hits x0.9, got " + softer);
        });
    }

    /** The set's "followers": at most 3 small animals from #femboymod:cute_followers; horses stay put. */
    public static void onlyAFewCuteAnimalsFollow(GameTestHelper helper) {
        Horse horse = helper.spawn(EntityType.HORSE, MOB_POS.offset(4, 0, 4));
        List<Cat> cats = new java.util.ArrayList<>();
        for (int i = 0; i < CATS; i++) {
            cats.add(helper.spawn(EntityType.CAT, MOB_POS.offset(5, 0, 1)));
        }
        WearableGameTests.withPlayer(helper, player -> {
            player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5)));
            var effect = new BuiltinEffects.FollowPassiveEffect(16, 1.0, 1, 2.0,
                    net.minecraft.core.HolderSet.empty(), 3);
            var picked = effect.pickFollowers(player);
            helper.assertFalse(picked.contains(horse), "horses do not follow");
            helper.assertValueEqual(picked.size(), 3, "only the 3 nearest cats follow");
            helper.assertTrue(cats.containsAll(picked), "the followers are cats");
        });
    }

    /** Programming socks: x1.5 against {@code #femboymod:bugs}, unchanged against zombies; gone when taken off. */
    public static void socksHitBugsHarder(GameTestHelper helper) {
        Bug bug = helper.spawnWithNoFreeWill(FemboyEntities.BUG.get(), MOB_POS);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, MOB_POS.east());
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
