package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.social.HeadpatConsent;
import dev.eliasnvx.femboymod.social.Headpats;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S: who may give this player headpats (their client option), sent on join and whenever it changes. */
public record HeadpatConsentPayload(HeadpatConsent consent) implements FemboyPacket {

    public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "headpat_consent");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(consent.ordinal());
    }

    /** An unknown value decodes as the strictest setting ({@link HeadpatConsent#byId}). */
    public static HeadpatConsentPayload read(FriendlyByteBuf buf) {
        return new HeadpatConsentPayload(HeadpatConsent.byId(buf.readVarInt()));
    }

    public static void handle(HeadpatConsentPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                Headpats.setConsent(player, payload.consent());
            }
        });
    }
}
