package dev.eliasnvx.femboymod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Clothing Rack: shows up to 3 hanging items (SPEC §5.6); Thrifter's job site. Contents drop when broken. */
public class ClothingRackBlockEntity extends BlockEntity implements Container {

    public static final int SLOTS = 3;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public ClothingRackBlockEntity(BlockPos pos, BlockState state) {
        super(FemboyBlocks.CLOTHING_RACK_ENTITY.get(), pos, state);
    }

    /** Hangs one item from {@code held} on the first free hook. @return whether something was hung */
    public boolean hang(ItemStack held) {
        for (int i = 0; i < SLOTS; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, held.split(1));
                setChanged();
                return true;
            }
        }
        return false;
    }

    /** Takes the last hung item. @return the item, or empty */
    public ItemStack takeLast() {
        for (int i = SLOTS - 1; i >= 0; i--) {
            if (!items.get(i).isEmpty()) {
                ItemStack taken = items.get(i);
                items.set(i, ItemStack.EMPTY);
                setChanged();
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items, true);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return ContainerHelper.saveAllItems(new CompoundTag(), items, true);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    // Container over the item list (26.3: ListBackedContainer)

    @Override
    public int getContainerSize() {
        return items.size();
    }

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
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    // Placement info for the item renderer (26.3: ItemOwner)

    public Level level() {
        return level;
    }

    public Vec3 position() {
        return Vec3.atCenterOf(worldPosition);
    }

    public float getVisualRotationYInDegrees() {
        return getBlockState().getValue(ClothingRackBlock.FACING).getOpposite().toYRot();
    }
}
