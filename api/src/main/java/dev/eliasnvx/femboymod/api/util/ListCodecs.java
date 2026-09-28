package dev.eliasnvx.femboymod.api.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import org.jetbrains.annotations.ApiStatus;

/**
 * List codec helpers. The DataFixerUpper shipped with Minecraft 1.20.1 has no {@code Codec#listOf(min, max)}.
 */
@ApiStatus.Internal
public final class ListCodecs {

    private ListCodecs() {
    }

    /**
     * A list codec that only accepts lists with {@code min} to {@code max} elements.
     *
     * @param element the element codec
     * @param min     the smallest allowed size
     * @param max     the largest allowed size
     * @param <T>     the element type
     * @return the size-checked list codec
     */
    public static <T> Codec<List<T>> sized(Codec<T> element, int min, int max) {
        return element.listOf().flatXmap(list -> check(list, min, max), list -> check(list, min, max));
    }

    private static <T> DataResult<List<T>> check(List<T> list, int min, int max) {
        if (list.size() < min || list.size() > max) {
            return DataResult.error(() -> "List size " + list.size() + " is outside " + min + ".." + max);
        }
        return DataResult.success(list);
    }
}
