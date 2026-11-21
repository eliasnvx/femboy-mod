package dev.eliasnvx.femboymod.block;

import com.mojang.logging.LogUtils;
import dev.eliasnvx.femboymod.item.PlushBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ListBackedContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/** Wall-mounted shelf holding up to 3 plushies or small decor items; drops contents when broken. */
public class PlushShelfBlockEntity extends BlockEntity implements ListBackedContainer, ItemOwner {

    public static final int SLOTS = 3;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private long squishStartTick = Long.MIN_VALUE;
    private @Nullable PlushSquish.Kind squishKind;

    public PlushShelfBlockEntity(BlockPos pos, BlockState state) {
        super(FemboyBlocks.PLUSH_SHELF_ENTITY.get(), pos, state);
    }

    /** Places one item from {@code held} onto the first free slot. */
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

    /** Takes the last placed item, or empty. */
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items, true);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            ContainerHelper.saveAllItems(output, items, true);
            return output.buildResult();
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public Level level() {
        return level;
    }

    @Override
    public Vec3 position() {
        return Vec3.atCenterOf(worldPosition);
    }

    @Override
    public float getVisualRotationYInDegrees() {
        return getBlockState().getValue(PlushShelfBlock.FACING).toYRot();
    }
}
