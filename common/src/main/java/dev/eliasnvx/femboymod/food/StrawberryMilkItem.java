package dev.eliasnvx.femboymod.food;

import java.util.function.Supplier;
import net.minecraft.world.level.ItemLike;
import dev.eliasnvx.femboymod.energy.FemboyEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Strawberry milk: calms you down after too much Byte Energy (removes Jitter and the coming caffeine crash). */
public class StrawberryMilkItem extends DrinkItem {

    /** @param container left behind after drinking (1.21.1: the food's usingConvertsTo) */
    public StrawberryMilkItem(Properties properties, Supplier<? extends ItemLike> container) {
        super(properties, container);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            boolean calmed = entity.removeEffect(FemboyEffects.holder(FemboyEffects.JITTER)) | entity.removeEffect(FemboyEffects.holder(FemboyEffects.CAFFEINATED));
            if (calmed && entity instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("message.femboymod.strawberry_milk"), true);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
