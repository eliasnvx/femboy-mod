package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.backpack.BackpackMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S: backpack key pressed (open the worn backpack, else the one in the main hand). */
public record OpenBackpackPayload() implements FemboyPacket {

    public static final OpenBackpackPayload INSTANCE = new OpenBackpackPayload();
    public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "open_backpack");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        // no data
    }

    public static OpenBackpackPayload read(FriendlyByteBuf buf) {
        return INSTANCE;
    }

    public static void handle(OpenBackpackPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player && player.isAlive() && player.containerMenu == player.inventoryMenu) {
                BackpackMenus.openFromKey(player);
            }
        });
    }
}
