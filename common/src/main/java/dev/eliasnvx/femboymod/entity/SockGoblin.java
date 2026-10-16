package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.block.FemboyBlocks;
import dev.eliasnvx.femboymod.block.LaundryBasketBlockEntity;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.registry.FemboySounds;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import dev.eliasnvx.femboymod.world.FemboyGameRules;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Sock Goblin: a small night prowler, the answer to where socks go. It sneaks up on a player, snatches one pair of
 * socks they carry but don't wear ({@code #femboymod:sock_goblin_loot}) and runs. Catch it to get them back, plus a
 * Lost Sock (loot table). A Laundry Basket with socks in it nearby is raided first. It never attacks, never
 * despawns while it holds someone's socks, stays in peaceful, and only steals while the game rule
 * {@code femboymod:sock_goblin_theft} is on.
 */
public class SockGoblin extends Monster {

    private static final double STEAL_REACH = 1.6;
    private static final double SEARCH_RANGE = 16.0;
    private static final double SNEAK_SPEED = 1.1;
    private static final double FLEE_SPEED = 1.45;
    private static final double FLEE_SPRINT = 1.65;
    private static final float FLEE_DISTANCE = 16.0F;
    private static final float LOOK_DISTANCE = 8.0F;
    private static final double FOLLOW_RANGE = 24.0;
    private static final int REPATH_TICKS = 10;
    private static final float STEAL_PITCH = 1.3F;
    /** Reach to a basket's middle (it stands on the floor, the goblin next to it). */
    private static final double BASKET_REACH = 1.9;
    private static final int BASKET_SEARCH_INTERVAL = 20;

    public SockGoblin(EntityType<? extends SockGoblin> type, Level level) {
        super(type, level);
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes(CommonConfig.Mob config) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, config.health())
                .add(Attributes.ATTACK_DAMAGE, config.attackDamage())
                .add(Attributes.MOVEMENT_SPEED, config.speed())
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, FLEE_SPEED));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, FLEE_DISTANCE, FLEE_SPEED, FLEE_SPRINT,
                player -> hasLoot()));
        goalSelector.addGoal(3, new RaidBasketGoal());
        goalSelector.addGoal(4, new StealSocksGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, LOOK_DISTANCE));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Holding someone's socks. */
    public boolean hasLoot() {
        return !getMainHandItem().isEmpty();
    }

    /** Inventory slot (main inventory and hotbar) of the first stealable item, or -1. */
    public static int findLoot(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (inventory.getItem(slot).is(FemboyTags.SOCK_GOBLIN_LOOT)) {
                return slot;
            }
        }
        return -1;
    }

    /** Slot of the first stealable item in a container (a laundry basket), or -1. */
    public static int findLoot(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).is(FemboyTags.SOCK_GOBLIN_LOOT)) {
                return slot;
            }
        }
        return -1;
    }

    /** Empty-handed, on the server, with the theft game rule on. */
    private boolean mayThieve() {
        return !hasLoot() && level() instanceof ServerLevel level && level.getGameRules().getBoolean(FemboyGameRules.SOCK_GOBLIN_THEFT);
    }

    /** Whether this goblin may rob this player right now. */
    public boolean canRob(Player player) {
        return mayThieve() && !player.isCreative() && !player.isSpectator() && player.isAlive() && findLoot(player) >= 0;
    }

    /** Takes one stealable item from the player's inventory into its hand (server); false if there was none. */
    public boolean steal(Player player) {
        int slot = findLoot(player);
        if (slot < 0 || hasLoot()) {
            return false;
        }
        ItemStack loot = player.getInventory().removeItem(slot, 1);
        hold(loot);
        player.displayClientMessage(Component.translatable("message.femboymod.sock_goblin.stole", loot.getHoverName()), true);
        return true;
    }

    /** Takes one stealable item out of a laundry basket (server); false if there was none. */
    public boolean raid(Container basket) {
        int slot = findLoot(basket);
        if (slot < 0 || hasLoot()) {
            return false;
        }
        hold(basket.removeItem(slot, 1));
        basket.setChanged();
        for (Player player : level().players()) {
            if (distanceToSqr(player) < SEARCH_RANGE * SEARCH_RANGE) {
                player.displayClientMessage(Component.translatable("message.femboymod.sock_goblin.raided"), true);
            }
        }
        return true;
    }

    private void hold(ItemStack loot) {
        setItemSlot(EquipmentSlot.MAINHAND, loot);
        setGuaranteedDrop(EquipmentSlot.MAINHAND);
        setPersistenceRequired();
        playSound(FemboySounds.GOBLIN_STEAL.get(), 1.0F, STEAL_PITCH);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !hasLoot() && super.removeWhenFarAway(distance);
    }

    /** Stays in peaceful (26.3: the type is not {@code notInPeaceful}): it never attacks and may hold someone's socks. */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return FemboySounds.GOBLIN_GIGGLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FemboySounds.GOBLIN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FemboySounds.GOBLIN_DEATH.get();
    }

    /**
     * Laundry first: a goblin goes for the nearest Laundry Basket with socks in it before it robs anyone, so a basket
     * by the door keeps the socks in your pockets safe.
     */
    final class RaidBasketGoal extends Goal {

        private @Nullable BlockPos basket;
        private int nextSearch;
        private int repath;

        RaidBasketGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!mayThieve() || --nextSearch > 0 || !(level() instanceof ServerLevel level)) {
                return false;
            }
            nextSearch = reducedTickDelay(BASKET_SEARCH_INTERVAL);
            basket = level.getPoiManager().findClosest(poi -> poi.is(FemboyBlocks.LAUNDRY_BASKET_POI), this::hasLoot,
                    blockPosition(), (int) SEARCH_RANGE, PoiManager.Occupancy.ANY).orElse(null);
            return basket != null;
        }

        private boolean hasLoot(BlockPos pos) {
            return level().getBlockEntity(pos) instanceof LaundryBasketBlockEntity container && findLoot(container) >= 0;
        }

        @Override
        public boolean canContinueToUse() {
            return basket != null && mayThieve() && hasLoot(basket)
                    && distanceToSqr(Vec3.atCenterOf(basket)) < SEARCH_RANGE * SEARCH_RANGE * 2;
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void stop() {
            basket = null;
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (basket == null) {
                return;
            }
            Vec3 center = Vec3.atCenterOf(basket);
            getLookControl().setLookAt(center);
            if (--repath <= 0) {
                repath = adjustedTickDelay(REPATH_TICKS);
                getNavigation().moveTo(center.x, basket.getY(), center.z, SNEAK_SPEED);
            }
            if (distanceToSqr(center) < BASKET_REACH * BASKET_REACH
                    && level().getBlockEntity(basket) instanceof LaundryBasketBlockEntity container && raid(container)) {
                getNavigation().stop();
            }
        }
    }

    /** Sneaks up on the nearest player who carries socks and grabs them. */
    final class StealSocksGoal extends Goal {

        private @Nullable Player target;
        private int repath;

        StealSocksGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            target = null;
            double best = SEARCH_RANGE * SEARCH_RANGE;
            for (Player player : level().players()) {
                double distance = distanceToSqr(player);
                if (distance < best && canRob(player)) {
                    best = distance;
                    target = player;
                }
            }
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && canRob(target) && distanceToSqr(target) < SEARCH_RANGE * SEARCH_RANGE * 2;
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void stop() {
            target = null;
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target);
            if (--repath <= 0) {
                repath = adjustedTickDelay(REPATH_TICKS);
                getNavigation().moveTo(target, SNEAK_SPEED);
            }
            if (distanceToSqr(target) < STEAL_REACH * STEAL_REACH && steal(target)) {
                getNavigation().stop();
            }
        }
    }
}
