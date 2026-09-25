package dev.eliasnvx.femboymod.backpack;

import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Where the open backpack lives: the back cosmetic slot ({@code worn}) or an inventory slot (hand).
 *
 * @param slot inventory index when not worn
 * @param rows capacity, sent to the client so it can build the same slots
 */
public record BackpackMenuData(boolean worn, int slot, int rows) {

    public static final StreamCodec<ByteBuf, BackpackMenuData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, BackpackMenuData::worn,
            ByteBufCodecs.VAR_INT, BackpackMenuData::slot,
            ByteBufCodecs.VAR_INT, BackpackMenuData::rows,
            BackpackMenuData::new);

    public ItemStack resolve(Player player) {
        if (worn) {
            return CosmeticsManager.get(player).get(FemboySlots.BACK);
        }
        return slot >= 0 && slot < player.getInventory().getContainerSize() ? player.getInventory().getItem(slot) : ItemStack.EMPTY;
    }
}
