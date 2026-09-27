package dev.eliasnvx.femboymod.vibe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.FemboyMod;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.UUID;

/** Best Vibe Check per player, highest first (world save data, so offline players stay on the board). */
public final class VibeLeaderboard extends SavedData {

    /** One player's best scan; the name is refreshed on every scan. */
    public record Entry(UUID player, String name, int score) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(Entry::player),
                Codec.STRING.fieldOf("name").forGetter(Entry::name),
                Codec.intRange(0, 100).fieldOf("score").forGetter(Entry::score)
        ).apply(i, Entry::new));
    }

    public static final Codec<VibeLeaderboard> CODEC = Entry.CODEC.listOf()
            .xmap(VibeLeaderboard::new, board -> List.copyOf(board.entries));

    /** Vanilla requires a data fixer type; this simple list needs no fixes, command storage's fixer leaves it alone. */
    public static final SavedDataType<VibeLeaderboard> TYPE = new SavedDataType<>(
            ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "vibe_leaderboard"), VibeLeaderboard::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final List<Entry> entries = new ArrayList<>();

    public VibeLeaderboard() {
    }

    private VibeLeaderboard(List<Entry> entries) {
        this.entries.addAll(entries);
    }

    public static VibeLeaderboard get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Records a scan; keeps the player's best and the top {@code size}. Returns the player's rank (1-based) or 0. */
    public int record(UUID player, String name, int score, int size) {
        Entry previous = entries.stream().filter(e -> e.player().equals(player)).findFirst().orElse(null);
        if (previous != null) {
            entries.remove(previous);
        }
        entries.add(new Entry(player, name, previous == null ? score : Math.max(score, previous.score())));
        entries.sort(Comparator.comparingInt(Entry::score).reversed());
        while (entries.size() > size) {
            entries.removeLast();
        }
        setDirty();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).player().equals(player)) {
                return i + 1;
            }
        }
        return 0;
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }
}
