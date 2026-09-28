package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S: show/hide the item in one of your cosmetic slots (it stays worn). */
public record ToggleCosmeticHiddenPayload(ResourceLocation slot) implements FemboyPacket {

    public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "toggle_cosmetic_hidden");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(slot);
    }

    public static ToggleCosmeticHiddenPayload read(FriendlyByteBuf buf) {
        return new ToggleCosmeticHiddenPayload(buf.readResourceLocation());
    }

    public static void handle(ToggleCosmeticHiddenPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player && CosmeticsManager.orderedSlots().contains(payload.slot())) {
                CosmeticsManager.setHidden(player, payload.slot(), !CosmeticsManager.get(player).isHidden(payload.slot()));
            }
        });
    }
}
