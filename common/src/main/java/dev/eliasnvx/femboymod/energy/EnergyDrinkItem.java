package dev.eliasnvx.femboymod.energy;

import dev.eliasnvx.femboymod.food.DrinkItem;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

/** A can of Byte Energy; drinking it leaves the empty can behind. */
public class EnergyDrinkItem extends DrinkItem {

    public EnergyDrinkItem(Properties properties, Supplier<? extends ItemLike> emptyCan) {
        super(properties, emptyCan);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            EnergyDrinks.onDrink(serverLevel, entity, stack);
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
