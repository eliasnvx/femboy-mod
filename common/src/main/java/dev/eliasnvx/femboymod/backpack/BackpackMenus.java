package dev.eliasnvx.femboymod.backpack;

import dev.architectury.registry.menu.ExtendedMenuDataProvider;
import dev.architectury.registry.menu.MenuRegistry;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.api.event.cosmetic.BackpackOpenEvent;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Opening backpack menus (server-authoritative). */
public final class BackpackMenus {

    /** Inventory index of the offhand slot. */
    public static final int OFFHAND_SLOT = Inventory.SLOT_OFFHAND;

    private BackpackMenus() {
    }

    public static @Nullable BackpackSpec spec(ItemStack stack) {
        return stack.get(FemboyComponents.BACKPACK.get());
    }

    /** Key B: the worn backpack, or else one in the main hand. */
    public static void openFromKey(ServerPlayer player) {
        if (spec(CosmeticsManager.get(player).get(FemboySlots.BACK)) != null) {
            open(player, new BackpackMenuData(true, -1, 0));
        } else if (spec(player.getMainHandItem()) != null) {
            openFromHand(player, InteractionHand.MAIN_HAND);
        }
    }

    public static void openFromHand(ServerPlayer player, InteractionHand hand) {
        int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : OFFHAND_SLOT;
        open(player, new BackpackMenuData(false, slot, 0));
    }

    private static void open(ServerPlayer player, BackpackMenuData where) {
        ItemStack stack = where.resolve(player);
        BackpackSpec spec = spec(stack);
        if (spec == null || player.isSpectator()) {
            return;
        }
        if (FemboyMod.api().events().post(new BackpackOpenEvent(player, stack, where.worn())).isCancelled()) {
            return;
        }
        BackpackMenuData data = new BackpackMenuData(where.worn(), where.slot(), spec.rows());
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuDataProvider<BackpackMenuData>() {
            @Override
            public BackpackMenuData getExtraData(ServerPlayer p) {
                return data;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, BackpackMenuData> getExtraDataCodec() {
                return BackpackMenuData.STREAM_CODEC;
            }

            @Override
            public Component getDisplayName() {
                return stack.getHoverName();
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                return new BackpackMenu(id, inventory, data);
            }
        });
    }
}
