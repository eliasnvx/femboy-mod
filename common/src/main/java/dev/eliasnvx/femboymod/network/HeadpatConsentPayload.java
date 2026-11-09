package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.social.HeadpatConsent;
import dev.eliasnvx.femboymod.social.Headpats;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** C2S: who may give this player headpats (their client option), sent on join and whenever it changes. */
public record HeadpatConsentPayload(HeadpatConsent consent) implements CustomPacketPayload {

    public static final Type<HeadpatConsentPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "headpat_consent"));

    public static final StreamCodec<ByteBuf, HeadpatConsentPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            id -> new HeadpatConsentPayload(HeadpatConsent.byId(id)), payload -> payload.consent().ordinal());

    @Override
    public Type<HeadpatConsentPayload> type() {
        return TYPE;
    }

    public static void handle(HeadpatConsentPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                Headpats.setConsent(player, payload.consent());
            }
        });
    }
}
