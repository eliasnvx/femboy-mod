package dev.eliasnvx.femboymod.energy;

import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.List;

/** Game times of recent cans (player attachment, persistent, not kept on death). Pure logic, unit tested. */
public record CaffeineLog(List<Long> drinks) {

    public static final CaffeineLog EMPTY = new CaffeineLog(List.of());
    public static final Codec<CaffeineLog> CODEC = Codec.LONG.listOf().xmap(CaffeineLog::new, CaffeineLog::drinks);

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
        return new CaffeineLog(kept);
    }

    public int count() {
        return drinks.size();
    }
}
