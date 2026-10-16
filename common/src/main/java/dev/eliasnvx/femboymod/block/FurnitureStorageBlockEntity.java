package dev.eliasnvx.femboymod.block;

import dev.eliasnvx.femboymod.menu.FurnitureStorageMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Small furniture storage (Laundry Basket, Mini Fridge): {@link #SIZE} slots that only take items in
 * {@link #accepts()} (menu slots and hoppers alike). The lid or door is open (block state {@code open}) while someone
 * has the menu open, like a barrel. Contents drop when it is broken ({@link FurnitureStorageBlock}).
 */
public abstract class FurnitureStorageBlockEntity extends BaseContainerBlockEntity {

    public static final int SIZE = 9;
    private static final float SOUND_VOLUME = 0.5F;
    private static final float PITCH_BASE = 0.9F;
    private static final float PITCH_SPREAD = 0.1F;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            playSound(state, openSound());
            setOpen(state, true);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            playSound(state, closeSound());
            setOpen(state, false);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof FurnitureStorageMenu menu && menu.container() == FurnitureStorageBlockEntity.this;
        }
    };

    protected FurnitureStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** What may go in. */
    protected abstract TagKey<Item> accepts();

    protected abstract MenuType<FurnitureStorageMenu> menuType();

    protected abstract SoundEvent openSound();

    protected abstract SoundEvent closeSound();

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(accepts());
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    // 1.20.1 BaseContainerBlockEntity has no getItems/setItems: the Container methods are spelled out (as in Wardrobe)

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new FurnitureStorageMenu(menuType(), id, inventory, this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void startOpen(Player player) {
        if (!remove && !player.isSpectator()) {
            openers.incrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!remove && !player.isSpectator()) {
            openers.decrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
        }
    }

    /** Scheduled block tick: closes the lid if the players who had it open walked away. */
    public void recheckOpen() {
        if (!remove) {
            openers.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
        }
    }

    private void setOpen(BlockState state, boolean open) {
        if (level != null && state.hasProperty(BlockStateProperties.OPEN)) {
            level.setBlockAndUpdate(getBlockPos(), state.setValue(BlockStateProperties.OPEN, open));
        }
    }

    private void playSound(BlockState state, SoundEvent sound) {
        if (level != null) {
            level.playSound(null, getBlockPos(), sound, SoundSource.BLOCKS, SOUND_VOLUME,
                    level.getRandom().nextFloat() * PITCH_SPREAD + PITCH_BASE);
        }
    }
}
