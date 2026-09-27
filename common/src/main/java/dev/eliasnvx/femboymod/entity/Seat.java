package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.api.profile.FemboyProfileFields;
import dev.eliasnvx.femboymod.api.event.profile.SetupRatedEvent;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.block.GamerChairBlock;
import dev.eliasnvx.femboymod.block.SetupRating;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Invisible seat for the Gamer Chair. It disappears when its rider stands up or the chair is gone,
 * and slowly heals a sitting player (config {@code furniture.chair_heal_*}). A better setup around the chair
 * ({@link SetupRating}) heals faster.
 */
public class Seat extends Entity {

    /** "Just need a break": sitting down at one heart or less. */
    private static final float LOW_HEALTH = 2.0F;

    private int setupRating;
    private int healTimer;

    public Seat(EntityType<? extends Seat> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** Seats a player on the chair at {@code pos}; returns false if someone already sits there. */
    public static boolean sit(ServerLevel level, BlockPos pos, Player player, double seatHeight) {
        if (!level.getEntitiesOfClass(Seat.class, new AABB(pos)).isEmpty()) {
            return false;
        }
        Seat seat = FemboyEntities.SEAT.get().create(level);
        if (seat == null) {
            return false;
        }
        seat.setPos(pos.getX() + 0.5, pos.getY() + seatHeight, pos.getZ() + 0.5);
        level.addFreshEntity(seat);
        if (!player.startRiding(seat)) {
            seat.discard();
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (player.getHealth() <= LOW_HEALTH) {
                FemboyTriggers.fire(serverPlayer, FemboyTriggers.CHAIR_BREAK);
            }
            seat.rateSetup(level, serverPlayer, true);
        }
        return true;
    }

    /** Stars of the setup around this seat, as last rated. */
    public int setupRating() {
        return setupRating;
    }

    private void rateSetup(ServerLevel level, ServerPlayer player, boolean announce) {
        int rating = SetupRating.evaluate(level, blockPosition(), FemboyConfig.common().furniture().setupRadius());
        if (announce || rating != setupRating) {
            player.displayClientMessage(SetupRating.message(rating), true);
            FemboyTriggers.fire(player, FemboyTriggers.SETUP_RATING, rating);
            FemboyMod.api().getProfile(player).update(FemboyProfileFields.BEST_SETUP_RATING, best -> Math.max(best, rating));
            FemboyMod.api().events().post(new SetupRatedEvent(player, blockPosition(), rating, SetupRating.MAX));
        }
        setupRating = rating;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (getPassengers().isEmpty() || !(level().getBlockState(blockPosition()).getBlock() instanceof GamerChairBlock)) {
            ejectPassengers();
            discard();
            return;
        }
        CommonConfig.Furniture config = FemboyConfig.common().furniture();
        if (config.chairHealInterval() <= 0 || !(getFirstPassenger() instanceof ServerPlayer player)) {
            return;
        }
        if (tickCount % config.chairHealInterval() == 0) {
            rateSetup(level, player, false); // re-rate now and then: the player may add or turn on pieces
        }
        int interval = Math.max(1, Math.round(config.chairHealInterval() / (1.0F + setupRating * config.setupHealBonus())));
        if (++healTimer >= interval) {
            healTimer = 0;
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(config.chairHealAmount());
            }
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide()) {
            discard();
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return Vec3.atBottomCenterOf(blockPosition().above());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
