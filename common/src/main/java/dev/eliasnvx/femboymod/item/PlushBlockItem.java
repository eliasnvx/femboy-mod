package dev.eliasnvx.femboymod.item;

import dev.eliasnvx.femboymod.block.PlushBlock;
import dev.eliasnvx.femboymod.registry.FemboySounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * Plushie item: placeable as a block, squeezable in the air (right-click to squeak),
 * huggable in third person, and wearable on the head via the helmet slot.
 */
public class PlushBlockItem extends BlockItem {

    private static final int SQUEEZE_COOLDOWN = 8;
    private static final float MIN_PITCH = 1.3F;
    private static final float PITCH_RANGE = 0.4F;

    public PlushBlockItem(Block block, Properties properties) {
        super(block, properties.useBlockDescriptionPrefix()
                .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setSwappable(false).build()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            float pitch = MIN_PITCH + level.getRandom().nextFloat() * PITCH_RANGE;
            level.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                    FemboySounds.PLUSH_SQUEAK.get(), SoundSource.PLAYERS, 0.9F, pitch);
            Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(0.8));
            serverLevel.sendParticles(ParticleTypes.HEART, front.x, front.y - 0.15, front.z, 1, 0.1, 0.1, 0.1, 0.0);
            if (getBlock() instanceof PlushBlock plush) {
                SimpleParticleType extra = plush.extraParticle();
                if (extra != null) {
                    serverLevel.sendParticles(extra, front.x, front.y - 0.2, front.z, 4, 0.18, 0.18, 0.18, 0.04);
                }
            }
        }
        player.getCooldowns().addCooldown(stack, SQUEEZE_COOLDOWN);
        return InteractionResult.SUCCESS;
    }
}
