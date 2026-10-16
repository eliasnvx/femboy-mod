package dev.eliasnvx.femboymod.block;

import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.menu.FurnitureStorageMenu;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyMenus;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Mini Fridge storage: drinks and snacks ({@code #femboymod:fridge_food}). A chillable item that sits in one slot for
 * {@code furniture.fridge_chill_ticks} gets the {@code femboymod:chilled} component (chilled Byte Energy buffs last
 * longer). Each slot's timer restarts when its contents change.
 */
public class MiniFridgeBlockEntity extends FurnitureStorageBlockEntity {

    private final int[] chillTicks = new int[SIZE];

    public MiniFridgeBlockEntity(BlockPos pos, BlockState state) {
        super(FemboyBlocks.MINI_FRIDGE_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MiniFridgeBlockEntity fridge) {
        fridge.chill(FemboyConfig.common().furniture().fridgeChillTicks());
    }

    /** One tick of chilling: every warm chillable stack gets a tick closer to cold. */
    public void chill(int ticksToChill) {
        boolean changed = false;
        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty() || !stack.is(FemboyTags.CHILLABLE) || stack.has(FemboyComponents.CHILLED.get())) {
                chillTicks[slot] = 0;
                continue;
            }
            if (++chillTicks[slot] >= ticksToChill) {
                stack.set(FemboyComponents.CHILLED.get(), Unit.INSTANCE);
                chillTicks[slot] = 0;
                changed = true;
            }
        }
        if (changed) {
            setChanged();
        }
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        chillTicks[slot] = 0;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        chillTicks[slot] = 0;
        return super.removeItem(slot, count);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.femboymod.mini_fridge");
    }

    @Override
    protected TagKey<Item> accepts() {
        return FemboyTags.FRIDGE_FOOD;
    }

    @Override
    protected MenuType<FurnitureStorageMenu> menuType() {
        return FemboyMenus.MINI_FRIDGE.get();
    }

    @Override
    protected SoundEvent openSound() {
        return SoundEvents.IRON_DOOR_OPEN;
    }

    @Override
    protected SoundEvent closeSound() {
        return SoundEvents.IRON_DOOR_CLOSE;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Arrays.fill(chillTicks, 0);
        input.getIntArray("chill_ticks").ifPresent(saved -> System.arraycopy(saved, 0, chillTicks, 0, Math.min(saved.length, SIZE)));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putIntArray("chill_ticks", chillTicks);
    }
}
