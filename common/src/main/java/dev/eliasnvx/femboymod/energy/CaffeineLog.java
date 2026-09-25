package dev.eliasnvx.femboymod.energy;

import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.List;

/** Game times of recent cans (player attachment, persistent, not kept on death). Pure logic, unit tested. */
public record CaffeineLog(List<Long> drinks, int total) {

    public static final CaffeineLog EMPTY = new CaffeineLog(List.of(), 0);
    public static final Codec<CaffeineLog> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.listOf().fieldOf("recent").forGetter(CaffeineLog::drinks),
            Codec.INT.optionalFieldOf("total", 0).forGetter(CaffeineLog::total)
    ).apply(i, CaffeineLog::new));

    public CaffeineLog {
        drinks = List.copyOf(drinks);
    }

    /** Adds a can at {@code now}, dropping cans older than {@code window}. */
    public CaffeineLog drink(long now, int window) {
        List<Long> kept = new ArrayList<>(drinks.size() + 1);
        for (long time : drinks) {
            if (now - time < window) {
                kept.add(time);
            }
        }
        kept.add(now);
        return new CaffeineLog(kept, total + 1);
    }

    public int count() {
        return drinks.size();
    }
}
