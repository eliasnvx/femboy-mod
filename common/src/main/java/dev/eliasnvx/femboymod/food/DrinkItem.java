package dev.eliasnvx.femboymod.food;

import java.util.function.Supplier;
import net.minecraft.advancements.CriteriaTriggers;
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
 * A drink. Minecraft 1.20.1 has no consumable component, so this class does what 26.3's
 * {@code Consumables.defaultDrink()} did: drink animation and sound, and for drinks without food
 * (Byte Energy) the vanilla drink time. Every drink leaves its container behind (1.20.1 food has no
 * {@code usingConvertsTo}, so food drinks get it back here too).
 */
public class DrinkItem extends Item {

    /** Vanilla's default drink time: 1.6 s, like 26.3's {@code Consumables.defaultDrink()}. */
    public static final int DRINK_TICKS = 32;

    private final @Nullable Supplier<? extends ItemLike> remainder;

    /** A drink that leaves nothing behind. */
    public DrinkItem(Properties properties) {
        this(properties, null);
    }

    /**
     * A drink that leaves {@code remainder} behind. Without food properties it can always be drunk;
     * with them, the food decides use time and whether the player may drink.
     */
    public DrinkItem(Properties properties, @Nullable Supplier<? extends ItemLike> remainder) {
        super(properties);
        this.remainder = remainder;
    }

    /** @return the container left behind, or empty */
    public ItemStack remainder() {
        return remainder == null ? ItemStack.EMPTY : new ItemStack(remainder.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (isEdible()) {
            return super.use(level, player, hand);
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return isEdible() ? super.getUseDuration(stack) : DRINK_TICKS;
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
        boolean creative = entity instanceof Player player && player.getAbilities().instabuild;
        if (isEdible()) {
            stack = super.finishUsingItem(stack, level, entity); // eats, shrinks (not in creative), triggers advancements
        } else {
            if (entity instanceof ServerPlayer player) {
                CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
                player.awardStat(Stats.ITEM_USED.get(this));
            }
            if (!creative) {
                stack.shrink(1);
            }
        }
        if (remainder == null) {
            return stack;
        }
        if (stack.isEmpty()) {
            return new ItemStack(remainder.get());
        }
        if (entity instanceof Player player && !creative) {
            ItemStack empty = new ItemStack(remainder.get());
            if (!player.getInventory().add(empty)) {
                player.drop(empty, false);
            }
        }
        return stack;
    }
}
