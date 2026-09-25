package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.menu.CosmeticsMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

/** C2S: the player asks to open the cosmetic slots screen. Carries no data. */
public record OpenCosmeticsMenuPayload() implements CustomPacketPayload {

    public static final OpenCosmeticsMenuPayload INSTANCE = new OpenCosmeticsMenuPayload();

    public static final Type<OpenCosmeticsMenuPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "open_cosmetics"));

    public static final StreamCodec<ByteBuf, OpenCosmeticsMenuPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<OpenCosmeticsMenuPayload> type() {
        return TYPE;
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
