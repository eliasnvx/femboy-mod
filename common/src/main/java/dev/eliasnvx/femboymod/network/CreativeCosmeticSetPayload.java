package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.menu.CosmeticPanelActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * C2S, creative only: put {@code stack} (or nothing) into a cosmetic slot. In the creative inventory the
 * cursor lives on the client, like vanilla's creative slot packet; creative players can create items anyway,
 * so the server only checks the game mode and that the item fits the slot.
 */
public record CreativeCosmeticSetPayload(ResourceLocation slot, ItemStack stack) implements FemboyPacket {

    public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "creative_cosmetic_set");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(slot);
        buf.writeItem(stack);
    }

    public static CreativeCosmeticSetPayload read(FriendlyByteBuf buf) {
        return new CreativeCosmeticSetPayload(buf.readResourceLocation(), buf.readItem());
    }

    public static void handle(CreativeCosmeticSetPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                CosmeticPanelActions.creativeSet(player, payload.slot(), payload.stack());
            }
        });
    }
}
