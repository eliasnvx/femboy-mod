package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.menu.CosmeticsMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

/** C2S: the player asks to open the cosmetic slots screen. Carries no data. */
public record OpenCosmeticsMenuPayload() implements FemboyPacket {

    public static final OpenCosmeticsMenuPayload INSTANCE = new OpenCosmeticsMenuPayload();

    public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "open_cosmetics");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        // no data
    }

    public static OpenCosmeticsMenuPayload read(FriendlyByteBuf buf) {
        return INSTANCE;
    }

    public static void handle(OpenCosmeticsMenuPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player && player.isAlive()) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inventory, p) -> new CosmeticsMenu(id, inventory),
                        Component.translatable("container.femboymod.cosmetics")));
            }
        });
    }
}
