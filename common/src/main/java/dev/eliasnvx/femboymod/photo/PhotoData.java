package dev.eliasnvx.femboymod.photo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ExtraCodecs;

/**
 * The {@code femboymod:photo} data of a printed photo: which picture (stored by the server, see {@link PhotoStore})
 * and who took it. 1.20.1 keeps it in the item's NBT (FemboyComponents.PHOTO), which syncs with the stack, so there
 * is no stream codec.
 */
public record PhotoData(UUID id, String author) {

    private static final int MAX_AUTHOR = 64;

    public static final Codec<PhotoData> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(PhotoData::id),
            ExtraCodecs.sizeLimitedString(0, MAX_AUTHOR).fieldOf("author").forGetter(PhotoData::author)
    ).apply(i, PhotoData::new));
}
