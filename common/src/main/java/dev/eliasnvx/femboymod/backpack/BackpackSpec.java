package dev.eliasnvx.femboymod.backpack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Component {@code femboymod:backpack}: marks an item as a backpack and sets its capacity (rows of 9). */
public record BackpackSpec(int rows) {

    public static final int MAX_ROWS = 6;
    public static final int CHARM_SLOTS = 3;

    public static final Codec<BackpackSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, MAX_ROWS).fieldOf("rows").forGetter(BackpackSpec::rows)
    ).apply(i, BackpackSpec::new));

    public int size() {
        return rows * 9;
    }
}
