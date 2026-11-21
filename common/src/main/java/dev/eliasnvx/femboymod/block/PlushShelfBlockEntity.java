package dev.eliasnvx.femboymod.block;

import dev.eliasnvx.femboymod.item.PlushBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Wall-mounted shelf holding up to 3 plushies or small decor items; drops contents when broken. */
public class PlushShelfBlockEntity extends BlockEntity implements Container {

    public static final int SLOTS = 3;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private long squishStartTick = Long.MIN_VALUE;
    private @Nullable PlushSquish.Kind squishKind;

    public PlushShelfBlockEntity(BlockPos pos, BlockState state) {
        super(FemboyBlocks.PLUSH_SHELF_ENTITY.get(), pos, state);
    }

    public boolean place(ItemStack held) {
        for (int i = 0; i < SLOTS; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, held.split(1));
                setChanged();
                return true;
            }
        }
        return false;
    }

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

    public boolean hasPlush() {
        for (ItemStack stack : items) {
            if (stack.getItem() instanceof PlushBlockItem) {
                return true;
            }
        }
        return false;
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == PlushBlockEntity.EVENT_CLICK) {
            this.squishKind = PlushSquish.Kind.CLICK;
            this.squishStartTick = level != null ? level.getGameTime() : 0L;
            return true;
        }
        if (id == PlushBlockEntity.EVENT_PLACE) {
            this.squishKind = PlushSquish.Kind.PLACE;
            this.squishStartTick = level != null ? level.getGameTime() : 0L;
            return true;
        }
        return super.triggerEvent(id, param);
    }

    public @Nullable PlushSquish.Kind squishKind() {
        return squishKind;
    }

    public float squishAge(float partialTicks) {
        if (squishKind == null || level == null || squishStartTick == Long.MIN_VALUE) {
            return -1.0F;
        }
        return (float) (level.getGameTime() - squishStartTick) + partialTicks;
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
        return saveWithoutMetadata();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override public int getContainerSize() { return SLOTS; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { ItemStack r = ContainerHelper.removeItem(items, slot, amount); if (!r.isEmpty()) setChanged(); return r; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); setChanged(); }
}
