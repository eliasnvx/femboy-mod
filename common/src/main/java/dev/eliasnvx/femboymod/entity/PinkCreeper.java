package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.event.PinkCreeperBlastEvent;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Pink Creeper (SPEC §5.4): hisses like a creeper, but bursts into confetti and hearts. No block damage,
 * players take 0 damage by default (config), everything nearby gets a playful push.
 * The vanilla explosion is replaced by {@code CreeperMixin} calling {@link #confettiBurst}.
 */
public class PinkCreeper extends Creeper {

    private static final int[] CONFETTI = {0xFF69B4, 0xF5A9B8, 0xC8A2E8, 0x5BCEFA, 0xFFFFFF, 0xFFD166};
    private static final int CONFETTI_PER_COLOR = 14;
    private static final float CONFETTI_SIZE = 1.2F;
    private static final int HEARTS = 12;
    private static final double SPREAD = 1.2;

    public PinkCreeper(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    /** Replaces the vanilla explosion. */
    public void confettiBurst(ServerLevel level) {
        CommonConfig.PinkCreeper config = FemboyConfig.common().pinkCreeper();
        PinkCreeperBlastEvent event = FemboyMod.api().events().post(new PinkCreeperBlastEvent(
                this, config.radius(), config.knockback(), config.playerDamage(), config.mobDamage()));
        this.dead = true;
        if (!event.isCancelled()) {
            double x = getX();
            double y = getY() + getBbHeight() * 0.6;
            double z = getZ();
            for (int color : CONFETTI) {
                level.sendParticles(new DustParticleOptions(color, CONFETTI_SIZE), x, y, z, CONFETTI_PER_COLOR, SPREAD, SPREAD, SPREAD, 0.1);
            }
            level.sendParticles(ParticleTypes.HEART, x, y, z, HEARTS, SPREAD, SPREAD * 0.5, SPREAD, 0.1);
            level.sendParticles(ParticleTypes.FIREWORK, x, y, z, HEARTS, 0.3, 0.3, 0.3, 0.2);
            level.playSound(null, x, y, z, dev.eliasnvx.femboymod.registry.FemboySounds.CONFETTI_POP.get(), getSoundSource(), 1.0F, 1.0F);
            level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_BLAST, getSoundSource(), 1.0F, 1.4F);

            double radius = event.radius();
            List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                    e -> e != this && e.isAlive() && e.distanceToSqr(this) <= radius * radius);
            for (LivingEntity target : hit) {
                boolean isPlayer = target instanceof Player;
                float damage = isPlayer ? event.playerDamage() : event.mobDamage();
                if (damage > 0) {
                    target.hurtServer(level, level.damageSources().explosion(this, this), damage);
                }
                target.knockback(event.knockback(), getX() - target.getX(), getZ() - target.getZ(),
                        level.damageSources().mobAttack(this), 0.0F);
                target.needsSync = true;
                if (target instanceof ServerPlayer player && player.isAlive()) {
                    FemboyTriggers.fire(player, FemboyTriggers.CONFETTI_SURVIVOR);
                }
            }
        }
        discard();
    }
}
