package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.photo.PhotoCodec;
import dev.eliasnvx.femboymod.photo.PhotoService;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Printed photos: the selfie goes up once, pictures come down when a client needs them. A picture is at most
 * {@link PhotoCodec#MAX_BYTES}, under 1.20.1's 32 KiB limit for client-to-server custom payloads.
 */
public final class PhotoPayloads {

    private PhotoPayloads() {
    }

    private static ResourceLocation channel(String path) {
        return new ResourceLocation(FemboyMod.MOD_ID, path);
    }

    /** C2S: a selfie to print ({@link PhotoCodec} bytes); the server checks paper, cooldown and the data. */
    public record Upload(byte[] data) implements FemboyPacket {
        public static final ResourceLocation ID = channel("photo_upload");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeByteArray(data);
        }

        public static Upload read(FriendlyByteBuf buf) {
            return new Upload(buf.readByteArray(PhotoCodec.MAX_BYTES));
        }

        public static void handle(Upload payload, NetworkManager.PacketContext context) {
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    PhotoService.upload(player, payload.data());
                }
            });
        }
    }

    /** C2S: the client wants the picture of a photo item it is showing. */
    public record Request(UUID photo) implements FemboyPacket {
        public static final ResourceLocation ID = channel("photo_request");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(photo);
        }

        public static Request read(FriendlyByteBuf buf) {
            return new Request(buf.readUUID());
        }

        public static void handle(Request payload, NetworkManager.PacketContext context) {
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    PhotoService.request(player, payload.photo());
                }
            });
        }
    }

    /** S2C: a picture ({@link PhotoCodec} bytes), or empty bytes if the server has none with that id. */
    public record Picture(UUID photo, byte[] data) implements FemboyPacket {
        public static final ResourceLocation ID = channel("photo_picture");
        /** Installed by the client entrypoint (turns the bytes into a texture); a no-op on servers. */
        public static BiConsumer<UUID, byte[]> onClient = (photo, data) -> { };

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(photo);
            buf.writeByteArray(data);
        }

        public static Picture read(FriendlyByteBuf buf) {
            return new Picture(buf.readUUID(), buf.readByteArray(PhotoCodec.MAX_BYTES));
        }

        public static void handle(Picture payload, NetworkManager.PacketContext context) {
            context.queue(() -> onClient.accept(payload.photo(), payload.data()));
        }
    }
}
