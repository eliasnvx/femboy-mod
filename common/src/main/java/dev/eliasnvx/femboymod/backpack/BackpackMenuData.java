package dev.eliasnvx.femboymod.backpack;

import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Where the open backpack lives: the back cosmetic slot ({@code worn}) or an inventory slot (hand).
 *
 * @param slot inventory index when not worn
 * @param rows capacity, sent to the client so it can build the same slots
 */
public record BackpackMenuData(boolean worn, int slot, int rows) {

    /** Extra data of the extended menu: worn flag, slot, rows. */
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(worn);
        buf.writeVarInt(slot);
        buf.writeVarInt(rows);
    }

    public static BackpackMenuData read(FriendlyByteBuf buf) {
        return new BackpackMenuData(buf.readBoolean(), buf.readVarInt(), buf.readVarInt());
    }

    public ItemStack resolve(Player player) {
        if (worn) {
            return CosmeticsManager.get(player).get(FemboySlots.BACK);
        }
        return slot >= 0 && slot < player.getInventory().getContainerSize() ? player.getInventory().getItem(slot) : ItemStack.EMPTY;
    }
}
