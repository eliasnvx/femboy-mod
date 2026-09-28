package dev.eliasnvx.femboymod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * A message of the mod. Minecraft 1.20.1 has no custom payload types: each message is a channel id plus
 * bytes written by {@link #write}; the matching reader is registered in {@link FemboyNetwork}.
 */
public interface FemboyPacket {

    /** Channel id the message is sent on. */
    ResourceLocation id();

    /** Writes the message body (the reader in {@link FemboyNetwork} must read the same fields in order). */
    void write(FriendlyByteBuf buf);
}
