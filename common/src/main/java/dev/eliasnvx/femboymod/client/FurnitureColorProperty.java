package dev.eliasnvx.femboymod.client;

import dev.architectury.registry.item.ItemPropertiesRegistry;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.block.DyedFurniture;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Item property {@code femboymod:color} of the dyeable furniture: the item's {@code color} block state as dye id / 16,
 * pink when the stack has none. 26.3 item definitions select the model on the {@code block_state} component; 1.20.1
 * item models pick the color's block model through overrides on this property instead
 * ({@code tools/decor/make_interior.py} writes both). The state comes from the stack's {@code BlockStateTag} NBT
 * (copied there by the loot table's {@code copy_state}, read back when the block is placed).
 */
public final class FurnitureColorProperty {

    public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "color");
    private static final float COLORS = DyeColor.values().length;

    private FurnitureColorProperty() {
    }

    /** For every block item of ours whose block has the {@link DyedFurniture#COLOR} state. */
    public static void register() {
        FemboyItems.REGISTER.forEach(supplier -> supplier.listen(item -> {
            if (item instanceof BlockItem blockItem && blockItem.getBlock().defaultBlockState().hasProperty(DyedFurniture.COLOR)) {
                ItemPropertiesRegistry.register(item, ID, FurnitureColorProperty::color);
            }
        }));
    }

    private static float color(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        CompoundTag state = stack.getTagElement(BlockItem.BLOCK_STATE_TAG);
        DyeColor color = state == null || !state.contains(DyedFurniture.COLOR.getName(), Tag.TAG_STRING) ? DyedFurniture.DEFAULT_COLOR
                : DyeColor.byName(state.getString(DyedFurniture.COLOR.getName()), DyedFurniture.DEFAULT_COLOR);
        return color.getId() / COLORS;
    }
}
