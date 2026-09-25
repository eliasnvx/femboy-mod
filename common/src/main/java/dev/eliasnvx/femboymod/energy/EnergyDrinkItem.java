package dev.eliasnvx.femboymod.energy;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A can of Byte Energy; the empty can is returned via the use-remainder component. */
public class EnergyDrinkItem extends Item {

    public EnergyDrinkItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            EnergyDrinks.onDrink(serverLevel, entity, stack);
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
