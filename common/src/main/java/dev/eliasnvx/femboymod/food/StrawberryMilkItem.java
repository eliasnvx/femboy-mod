package dev.eliasnvx.femboymod.food;

import dev.eliasnvx.femboymod.energy.FemboyEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Strawberry milk: calms you down after too much Byte Energy (removes Jitter and the coming caffeine crash). */
public class StrawberryMilkItem extends Item {

    public StrawberryMilkItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            boolean calmed = entity.removeEffect(FemboyEffects.JITTER.asHolder()) | entity.removeEffect(FemboyEffects.CAFFEINATED.asHolder());
            if (calmed && entity instanceof ServerPlayer player) {
                player.sendOverlayMessage(Component.translatable("message.femboymod.strawberry_milk"));
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
