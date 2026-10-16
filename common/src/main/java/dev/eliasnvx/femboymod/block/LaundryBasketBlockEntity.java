package dev.eliasnvx.femboymod.block;

import dev.eliasnvx.femboymod.menu.FurnitureStorageMenu;
import dev.eliasnvx.femboymod.registry.FemboyMenus;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

/** Laundry Basket storage: worn-in-a-slot clothes ({@code #femboymod:laundry}). Sock Goblins raid it first. */
public class LaundryBasketBlockEntity extends FurnitureStorageBlockEntity {

    public LaundryBasketBlockEntity(BlockPos pos, BlockState state) {
        super(FemboyBlocks.LAUNDRY_BASKET_ENTITY.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.femboymod.laundry_basket");
    }

    @Override
    protected TagKey<Item> accepts() {
        return FemboyTags.LAUNDRY;
    }

    @Override
    protected MenuType<FurnitureStorageMenu> menuType() {
        return FemboyMenus.LAUNDRY_BASKET.get();
    }

    @Override
    protected SoundEvent openSound() {
        return SoundEvents.BUNDLE_INSERT;
    }

    @Override
    protected SoundEvent closeSound() {
        return SoundEvents.BUNDLE_INSERT;
    }
}
