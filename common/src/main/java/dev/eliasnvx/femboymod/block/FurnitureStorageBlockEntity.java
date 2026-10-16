package dev.eliasnvx.femboymod.block;

import dev.eliasnvx.femboymod.menu.FurnitureStorageMenu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Small furniture storage (Laundry Basket, Mini Fridge): {@link #SIZE} slots that only take items in
 * {@link #accepts()} (menu slots and hoppers alike). The lid or door is open (block state {@code open}) while someone
 * has the menu open, like a barrel. Contents drop when it is broken (vanilla container removal).
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
        public boolean isOwnContainer(Player player) {
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
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new FurnitureStorageMenu(menuType(), id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!remove && !user.getLivingEntity().isSpectator()) {
            openers.incrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState(), user.getContainerInteractionRange());
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!remove && !user.getLivingEntity().isSpectator()) {
            openers.decrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState());
        }
    }

    public List<ContainerUser> getEntitiesWithContainerOpen() {
        return openers.getEntitiesWithContainerOpen(getLevel(), getBlockPos());
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
