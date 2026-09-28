package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.menu.CosmeticPanelActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S: a click on a cosmetic slot of the inventory panel; the server applies it to the real stacks. */
public record CosmeticPanelClickPayload(ResourceLocation slot, boolean quickMove) implements FemboyPacket {

    public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "cosmetic_panel_click");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(slot);
        buf.writeBoolean(quickMove);
    }

    public static CosmeticPanelClickPayload read(FriendlyByteBuf buf) {
        return new CosmeticPanelClickPayload(buf.readResourceLocation(), buf.readBoolean());
    }

    public static void handle(CosmeticPanelClickPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                CosmeticPanelActions.click(player, payload.slot(), payload.quickMove());
            }
        });
    }
}
