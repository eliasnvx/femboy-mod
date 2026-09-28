package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.backpack.BackpackMenus;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S: backpack key pressed (open the worn backpack, else the one in the main hand). */
public record OpenBackpackPayload() implements CustomPacketPayload {

    public static final OpenBackpackPayload INSTANCE = new OpenBackpackPayload();
    public static final Type<OpenBackpackPayload> TYPE = new Type<>(new ResourceLocation(FemboyMod.MOD_ID, "open_backpack"));
    public static final StreamCodec<ByteBuf, OpenBackpackPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<OpenBackpackPayload> type() {
        return TYPE;
    }

    public static void handle(OpenBackpackPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player && player.isAlive() && player.containerMenu == player.inventoryMenu) {
                BackpackMenus.openFromKey(player);
            }
        });
    }
}
