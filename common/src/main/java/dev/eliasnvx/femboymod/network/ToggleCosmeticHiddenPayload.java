package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S: show/hide the item in one of your cosmetic slots (it stays worn). */
public record ToggleCosmeticHiddenPayload(ResourceLocation slot) implements CustomPacketPayload {

    public static final Type<ToggleCosmeticHiddenPayload> TYPE =
            new Type<>(new ResourceLocation(FemboyMod.MOD_ID, "toggle_cosmetic_hidden"));

    public static final StreamCodec<ByteBuf, ToggleCosmeticHiddenPayload> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(ToggleCosmeticHiddenPayload::new, ToggleCosmeticHiddenPayload::slot);

    @Override
    public Type<ToggleCosmeticHiddenPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleCosmeticHiddenPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player && CosmeticsManager.orderedSlots().contains(payload.slot())) {
                CosmeticsManager.setHidden(player, payload.slot(), !CosmeticsManager.get(player).isHidden(payload.slot()));
            }
        });
    }
}
