package dev.eliasnvx.femboymod.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.20.1: the player's network connection is private in vanilla (Forge makes it public). Needed to skip packets
 * to players without a real channel (GameTest mock players): Forge throws when sending to them.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerAccessor {

    @Accessor("connection")
    Connection femboymod$connection();
}
