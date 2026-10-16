package dev.eliasnvx.femboymod.client;

import dev.architectury.registry.item.ItemPropertiesRegistry;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.block.DyedFurniture;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import org.jetbrains.annotations.Nullable;

/**
 * Item property {@code femboymod:color} of the dyeable furniture: the item's {@code color} block state as dye id / 16,
 * pink when the stack has none. 26.3 item definitions select the model on the {@code block_state} component; 1.21.1
 * item models pick the color's block model through overrides on this property instead
 * ({@code tools/decor/make_interior.py} writes both).
 */
public final class FurnitureColorProperty {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "color");
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
        BlockItemStateProperties state = stack.get(DataComponents.BLOCK_STATE);
        DyeColor color = state == null ? null : state.get(DyedFurniture.COLOR);
        return (color == null ? DyedFurniture.DEFAULT_COLOR : color).getId() / COLORS;
    }
}
