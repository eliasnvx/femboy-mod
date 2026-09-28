package dev.eliasnvx.femboymod.food;

import java.util.function.Supplier;
import net.minecraft.world.level.ItemLike;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Bubble tea: every cup is a surprise flavor ({@link BubbleTeaFlavor}, data-driven) with its own buff. */
public class BubbleTeaItem extends DrinkItem {

    /** @param container left behind after drinking (1.21.1: the food's usingConvertsTo) */
    public BubbleTeaItem(Properties properties, Supplier<? extends ItemLike> container) {
        super(properties, container);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            Holder.Reference<BubbleTeaFlavor> flavor = pick(serverLevel, serverLevel.getRandom());
            if (flavor != null) {
                flavor.value().effects().forEach(buff -> entity.addEffect(buff.instance()));
                if (entity instanceof ServerPlayer player) {
                    player.displayClientMessage(Component.translatable("message.femboymod.bubble_tea",
                            Component.translatable(flavor.key().location().toLanguageKey("bubble_tea_flavor"))), true);
                }
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    /** Weighted random flavor, or null if a data pack removed them all. */
    public static @Nullable Holder.Reference<BubbleTeaFlavor> pick(ServerLevel level, RandomSource random) {
        List<Holder.Reference<BubbleTeaFlavor>> flavors = level.registryAccess().lookupOrThrow(BubbleTeaFlavor.REGISTRY_KEY)
                .listElements().toList();
        int total = flavors.stream().mapToInt(flavor -> flavor.value().weight()).sum();
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (Holder.Reference<BubbleTeaFlavor> flavor : flavors) {
            roll -= flavor.value().weight();
            if (roll < 0) {
                return flavor;
            }
        }
        return flavors.get(flavors.size() - 1);
    }
}
