package dev.eliasnvx.femboymod.vibe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.FemboyMod;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
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

    /** File name under {@code data/} (1.21.1 saved data is named by a plain file name, not an id). */
    public static final String FILE_NAME = FemboyMod.MOD_ID + "_vibe_leaderboard";
    private static final String ENTRIES_KEY = "entries";
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Vanilla requires a data fixer type; this simple list needs no fixes, command storage's fixer leaves it alone. */
    public static final Factory<VibeLeaderboard> FACTORY = new Factory<>(
            VibeLeaderboard::new, VibeLeaderboard::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final List<Entry> entries = new ArrayList<>();

    public VibeLeaderboard() {
    }

    private VibeLeaderboard(List<Entry> entries) {
        this.entries.addAll(entries);
    }

    public static VibeLeaderboard get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_NAME);
    }

    private static VibeLeaderboard load(CompoundTag tag, HolderLookup.Provider registries) {
        Tag entries = tag.get(ENTRIES_KEY);
        if (entries == null) {
            return new VibeLeaderboard();
        }
        return CODEC.parse(NbtOps.INSTANCE, entries)
                .resultOrPartial(error -> LOGGER.error("Invalid Vibe Check leaderboard: {}", error))
                .orElseGet(VibeLeaderboard::new);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CODEC.encodeStart(NbtOps.INSTANCE, this)
                .resultOrPartial(error -> LOGGER.error("Could not save the Vibe Check leaderboard: {}", error))
                .ifPresent(entries -> tag.put(ENTRIES_KEY, entries));
        return tag;
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
