package dev.eliasnvx.femboymod.photo;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.FemboyMod;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

/**
 * Which printed pictures the server keeps and whose they are, least recently seen first (world save data). It caps the
 * photo folder (config {@code photos.max_per_player} / {@code max_total}): past a cap, the picture nobody has looked at
 * for the longest fades. A picture counts as seen when a client asks for it, so photos on display stay and the ones
 * lying forgotten in a chest go first. Pictures printed before the index existed are listed once, oldest file first,
 * with no author: they count toward the total only. Server thread only.
 */
public final class PhotoIndex extends SavedData {

    /** Author of pictures printed before the index existed. */
    static final UUID UNKNOWN_AUTHOR = Util.NIL_UUID;

    private record Entry(UUID photo, UUID author) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("photo").forGetter(Entry::photo),
                UUIDUtil.CODEC.fieldOf("author").forGetter(Entry::author)
        ).apply(i, Entry::new));
    }

    public static final Codec<PhotoIndex> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("scanned").orElse(false).forGetter(index -> index.scanned),
            Entry.CODEC.listOf().fieldOf("photos").orElse(List.of()).forGetter(PhotoIndex::entries)
    ).apply(i, PhotoIndex::new));

    /** File name under {@code data/} (1.21.1 saved data is named by a plain file name, not an id). */
    public static final String FILE_NAME = FemboyMod.MOD_ID + "_photo_index";
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Vanilla requires a data fixer type; this simple list needs no fixes, command storage's fixer leaves it alone. */
    public static final Factory<PhotoIndex> FACTORY = new Factory<>(PhotoIndex::new, PhotoIndex::load,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    /** Picture to author, in access order: the first one is the least recently seen. */
    private final LinkedHashMap<UUID, UUID> photos = new LinkedHashMap<>(16, 0.75F, true);
    private final Map<UUID, Integer> perAuthor = new HashMap<>();
    /** Whether pictures printed before the index existed have been listed. */
    private boolean scanned;

    /** An empty index; the world's own comes from {@link #get}. */
    public PhotoIndex() {
    }

    private PhotoIndex(boolean scanned, List<Entry> entries) {
        this.scanned = scanned;
        entries.forEach(entry -> put(entry.photo(), entry.author()));
    }

    /** This world's index; the first call lists the pictures already on disk. */
    public static PhotoIndex get(MinecraftServer server) {
        PhotoIndex index = server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_NAME);
        if (!index.scanned) {
            index.scanned = true;
            for (UUID photo : PhotoStore.storedOldestFirst(server)) {
                if (!index.photos.containsKey(photo)) {
                    index.put(photo, UNKNOWN_AUTHOR);
                }
            }
            index.setDirty();
        }
        return index;
    }

    /** 1.21.1 saved data reads and writes NBT itself: the codec's fields go straight into the file's data tag. */
    private static PhotoIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(error -> LOGGER.error("Invalid photo index: {}", error))
                .orElseGet(PhotoIndex::new);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CODEC.encodeStart(NbtOps.INSTANCE, this)
                .resultOrPartial(error -> LOGGER.error("Could not save the photo index: {}", error))
                .ifPresent(encoded -> tag.merge((CompoundTag) encoded));
        return tag;
    }

    private List<Entry> entries() {
        List<Entry> entries = new ArrayList<>(photos.size());
        photos.forEach((photo, author) -> entries.add(new Entry(photo, author)));
        return entries;
    }

    private void put(UUID photo, UUID author) {
        photos.put(photo, author);
        if (!UNKNOWN_AUTHOR.equals(author)) {
            perAuthor.merge(author, 1, Integer::sum);
        }
    }

    /**
     * Lists a new picture and returns the ones that fade to stay within the caps (0 = no cap): the author's least
     * recently seen past {@code maxPerPlayer}, then the server's past {@code maxTotal}. Never the new one.
     */
    public List<UUID> add(UUID photo, UUID author, int maxPerPlayer, int maxTotal) {
        put(photo, author);
        List<UUID> faded = new ArrayList<>();
        if (maxPerPlayer > 0) {
            Iterator<Map.Entry<UUID, UUID>> it = photos.entrySet().iterator();
            while (count(author) > maxPerPlayer && it.hasNext()) {
                Map.Entry<UUID, UUID> entry = it.next();
                if (entry.getValue().equals(author) && !entry.getKey().equals(photo)) {
                    it.remove();
                    perAuthor.merge(author, -1, Integer::sum);
                    faded.add(entry.getKey());
                }
            }
        }
        if (maxTotal > 0) {
            Iterator<Map.Entry<UUID, UUID>> it = photos.entrySet().iterator();
            while (photos.size() > maxTotal && it.hasNext()) {
                Map.Entry<UUID, UUID> entry = it.next();
                if (!entry.getKey().equals(photo)) {
                    it.remove();
                    if (!UNKNOWN_AUTHOR.equals(entry.getValue())) {
                        perAuthor.merge(entry.getValue(), -1, Integer::sum);
                    }
                    faded.add(entry.getKey());
                }
            }
        }
        setDirty();
        return faded;
    }

    /** Someone looked at this picture: it moves to the back of the line. */
    public void seen(UUID photo) {
        if (photos.get(photo) != null) {
            setDirty();
        }
    }

    public boolean contains(UUID photo) {
        return photos.containsKey(photo);
    }

    public int count(UUID author) {
        return perAuthor.getOrDefault(author, 0);
    }

    public int size() {
        return photos.size();
    }
}
