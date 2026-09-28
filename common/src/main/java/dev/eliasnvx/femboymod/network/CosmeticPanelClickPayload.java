package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.menu.CosmeticPanelActions;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S: a click on a cosmetic slot of the inventory panel; the server applies it to the real stacks. */
public record CosmeticPanelClickPayload(ResourceLocation slot, boolean quickMove) implements CustomPacketPayload {

    public static final Type<CosmeticPanelClickPayload> TYPE =
            new Type<>(new ResourceLocation(FemboyMod.MOD_ID, "cosmetic_panel_click"));

    public static final StreamCodec<ByteBuf, CosmeticPanelClickPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, CosmeticPanelClickPayload::slot,
            ByteBufCodecs.BOOL, CosmeticPanelClickPayload::quickMove,
            CosmeticPanelClickPayload::new);

    @Override
    public Type<CosmeticPanelClickPayload> type() {
        return TYPE;
    }

    public static void handle(CosmeticPanelClickPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                CosmeticPanelActions.click(player, payload.slot(), payload.quickMove());
            }
        });
    }
}
