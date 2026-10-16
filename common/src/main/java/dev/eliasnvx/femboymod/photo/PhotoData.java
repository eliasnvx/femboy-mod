package dev.eliasnvx.femboymod.photo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The {@code femboymod:photo} component of a printed photo: which picture (stored by the server, see
 * {@link PhotoStore}) and who took it.
 */
public record PhotoData(UUID id, String author) {

    private static final int MAX_AUTHOR = 64;

    public static final Codec<PhotoData> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(PhotoData::id),
            Codec.string(0, MAX_AUTHOR).fieldOf("author").forGetter(PhotoData::author)
    ).apply(i, PhotoData::new));

    public static final StreamCodec<ByteBuf, PhotoData> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PhotoData::id,
            ByteBufCodecs.stringUtf8(MAX_AUTHOR), PhotoData::author,
            PhotoData::new);
}
