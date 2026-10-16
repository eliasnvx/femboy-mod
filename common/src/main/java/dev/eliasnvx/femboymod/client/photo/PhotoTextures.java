package dev.eliasnvx.femboymod.client.photo;

import com.mojang.blaze3d.platform.NativeImage;
import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.network.PhotoPayloads;
import dev.eliasnvx.femboymod.photo.PhotoCodec;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Client cache of photo pictures as textures ({@code femboymod:photo/<id>}). The first look at a photo asks the
 * server for it; until it arrives (or if the server has none) {@link #texture} returns null and the photo shows as
 * still developing. At most {@link #MAX_TEXTURES} stay loaded; the least recently drawn go first.
 */
public final class PhotoTextures {

    private static final int MAX_TEXTURES = 64;
    /** Ticks before asking again for a picture the server didn't send (rate limit, slow disk). */
    private static final int RETRY_TICKS = 100;

    private static final class Entry {
        @Nullable Identifier texture;
        boolean missing;
        long requestedAt;
    }

    private static final Map<UUID, Entry> ENTRIES = new LinkedHashMap<>(16, 0.75F, true);
    private static long ticks;

    private PhotoTextures() {
    }

    public static void tick() {
        ticks++;
    }

    /** The loaded picture of a photo, or null while it isn't here (asks the server if needed). Render thread only. */
    public static @Nullable Identifier texture(UUID id) {
        Entry entry = ENTRIES.get(id);
        if (entry == null) {
            entry = new Entry();
            ENTRIES.put(id, entry);
            request(id, entry);
            evict();
        } else if (entry.texture == null && !entry.missing && ticks - entry.requestedAt > RETRY_TICKS) {
            request(id, entry);
        }
        return entry.texture;
    }

    private static void request(UUID id, Entry entry) {
        entry.requestedAt = ticks;
        if (Minecraft.getInstance().getConnection() != null) {
            NetworkManager.sendToServer(new PhotoPayloads.Request(id));
        }
    }

    /** A picture from the server (empty bytes: it has none). */
    public static void accept(UUID id, byte[] data) {
        Entry entry = ENTRIES.computeIfAbsent(id, uuid -> new Entry());
        if (entry.texture != null) {
            return;
        }
        int[] pixels = PhotoCodec.decode(data).orElse(null);
        if (pixels == null) {
            entry.missing = true;
            return;
        }
        NativeImage image = new NativeImage(PhotoCodec.SIZE, PhotoCodec.SIZE, false);
        for (int i = 0; i < pixels.length; i++) {
            image.setPixel(i % PhotoCodec.SIZE, i / PhotoCodec.SIZE, pixels[i]);
        }
        Identifier texture = Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "photo/" + id);
        Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(() -> "femboymod photo " + id, image));
        entry.texture = texture;
    }

    private static void evict() {
        Iterator<Map.Entry<UUID, Entry>> it = ENTRIES.entrySet().iterator();
        while (ENTRIES.size() > MAX_TEXTURES && it.hasNext()) {
            Entry eldest = it.next().getValue();
            if (eldest.texture != null) {
                Minecraft.getInstance().getTextureManager().release(eldest.texture);
            }
            it.remove();
        }
    }

    /** Leaving a world: pictures belong to that server. */
    public static void clear() {
        for (Entry entry : ENTRIES.values()) {
            if (entry.texture != null) {
                Minecraft.getInstance().getTextureManager().release(entry.texture);
            }
        }
        ENTRIES.clear();
    }
}
