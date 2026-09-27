package dev.eliasnvx.femboymod.food;

import java.util.function.Supplier;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A drink. Minecraft 1.21.1 has no consumable component, so this class does what 26.3's
 * {@code Consumables.defaultDrink()} did: drink animation and sound, and for drinks without food
 * (Byte Energy) the vanilla drink time and an empty container back. Drinks with food get their
 * container back through {@code FoodProperties.usingConvertsTo}.
 */
public class DrinkItem extends Item {

    /** Vanilla's default drink time: 1.6 s, like 26.3's {@code Consumables.defaultDrink()}. */
    public static final int DRINK_TICKS = 32;

    private final @Nullable Supplier<? extends ItemLike> remainder;

    /** A drink with food properties (the food component handles use time and the empty container). */
    public DrinkItem(Properties properties) {
        this(properties, null);
    }

    /** A drink without food that can always be drunk and leaves {@code remainder} behind. */
    public DrinkItem(Properties properties, @Nullable Supplier<? extends ItemLike> remainder) {
        super(properties);
        this.remainder = remainder;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).has(DataComponents.FOOD)) {
            return super.use(level, player, hand);
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return stack.has(DataComponents.FOOD) ? super.getUseDuration(stack, entity) : DRINK_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return getDrinkingSound(); // LivingEntity#eat plays the eating sound once the food is finished
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (stack.has(DataComponents.FOOD)) {
            return super.finishUsingItem(stack, level, entity);
        }
        if (entity instanceof ServerPlayer player) {
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        stack.consume(1, entity);
        if (remainder == null) {
            return stack;
        }
        if (stack.isEmpty()) {
            return new ItemStack(remainder.get());
        }
        if (entity instanceof Player player && !player.hasInfiniteMaterials()) {
            ItemStack empty = new ItemStack(remainder.get());
            if (!player.getInventory().add(empty)) {
                player.drop(empty, false);
            }
        }
        return stack;
    }
}
