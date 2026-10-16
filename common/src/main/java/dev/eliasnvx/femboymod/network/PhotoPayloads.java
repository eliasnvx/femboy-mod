package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.photo.PhotoCodec;
import dev.eliasnvx.femboymod.photo.PhotoService;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Printed photos: the selfie goes up once, pictures come down when a client needs them. */
public final class PhotoPayloads {

    private PhotoPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, path));
    }

    /** C2S: a selfie to print ({@link PhotoCodec} bytes); the server checks paper, cooldown and the data. */
    public record Upload(byte[] data) implements CustomPacketPayload {
        public static final Type<Upload> TYPE = payloadType("photo_upload");
        public static final StreamCodec<ByteBuf, Upload> STREAM_CODEC =
                ByteBufCodecs.byteArray(PhotoCodec.MAX_BYTES).map(Upload::new, Upload::data);

        @Override
        public Type<Upload> type() {
            return TYPE;
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
    public record Request(UUID id) implements CustomPacketPayload {
        public static final Type<Request> TYPE = payloadType("photo_request");
        public static final StreamCodec<ByteBuf, Request> STREAM_CODEC = UUIDUtil.STREAM_CODEC.map(Request::new, Request::id);

        @Override
        public Type<Request> type() {
            return TYPE;
        }

        public static void handle(Request payload, NetworkManager.PacketContext context) {
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    PhotoService.request(player, payload.id());
                }
            });
        }
    }

    /** S2C: a picture ({@link PhotoCodec} bytes), or empty bytes if the server has none with that id. */
    public record Picture(UUID id, byte[] data) implements CustomPacketPayload {
        public static final Type<Picture> TYPE = payloadType("photo_picture");
        public static final StreamCodec<ByteBuf, Picture> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Picture::id,
                ByteBufCodecs.byteArray(PhotoCodec.MAX_BYTES), Picture::data,
                Picture::new);
        /** Installed by the client entrypoint (turns the bytes into a texture); a no-op on servers. */
        public static BiConsumer<UUID, byte[]> onClient = (id, data) -> { };

        @Override
        public Type<Picture> type() {
            return TYPE;
        }

        public static void handle(Picture payload, NetworkManager.PacketContext context) {
            context.queue(() -> onClient.accept(payload.id(), payload.data()));
        }
    }
}
