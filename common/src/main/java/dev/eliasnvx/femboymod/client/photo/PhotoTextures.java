package dev.eliasnvx.femboymod.client.photo;

import com.mojang.blaze3d.platform.NativeImage;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.network.FemboyNetwork;
import dev.eliasnvx.femboymod.network.PhotoPayloads;
import dev.eliasnvx.femboymod.photo.PhotoCodec;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Client cache of photo pictures as textures ({@code femboymod:photo/<id>}). The first look at a photo asks the
 * server for it; until it arrives (or if the server has none) {@link #texture} returns null and the photo shows as
 * still developing. At most {@link #MAX_TEXTURES} stay loaded; the least recently drawn go first, but never one
 * drawn this tick or the last: with more photos in view than that, the extra ones stay developing instead of
 * evicting each other every frame (and asking the server again and again).
 */
public final class PhotoTextures {

    private static final int MAX_TEXTURES = 64;
    /** Ticks before asking again for a picture the server didn't send (rate limit, slow disk). */
    private static final int RETRY_TICKS = 100;

    private static final class Entry {
        @Nullable ResourceLocation texture;
        boolean missing;
        long requestedAt;
        long drawnAt;
    }

    private static final Map<UUID, Entry> ENTRIES = new LinkedHashMap<>(16, 0.75F, true);
    private static final Set<UUID> FADED = ConcurrentHashMap.newKeySet();
    private static long ticks;

    private PhotoTextures() {
    }

    public static void tick() {
        ticks++;
    }

    /** The loaded picture of a photo, or null while it isn't here (asks the server if needed). Render thread only. */
    public static @Nullable ResourceLocation texture(UUID id) {
        Entry entry = ENTRIES.get(id);
        if (entry == null) {
            if (ENTRIES.size() >= MAX_TEXTURES && !evictOne()) {
                return null; // all in view right now: this one waits, still developing
            }
            entry = new Entry();
            ENTRIES.put(id, entry);
            request(id, entry);
        } else if (entry.texture == null && !entry.missing && ticks - entry.requestedAt > RETRY_TICKS) {
            request(id, entry);
        }
        entry.drawnAt = ticks;
        return entry.texture;
    }

    private static void request(UUID id, Entry entry) {
        entry.requestedAt = ticks;
        if (Minecraft.getInstance().getConnection() != null) {
            FemboyNetwork.sendToServer(new PhotoPayloads.Request(id));
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
            FADED.add(id);
            return;
        }
        NativeImage image = new NativeImage(PhotoCodec.SIZE, PhotoCodec.SIZE, false);
        for (int i = 0; i < pixels.length; i++) {
            image.setPixelRGBA(i % PhotoCodec.SIZE, i / PhotoCodec.SIZE, abgr(pixels[i]));
        }
        ResourceLocation texture = new ResourceLocation(FemboyMod.MOD_ID, "photo/" + id);
        Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(image));
        entry.texture = texture;
    }

    /** 1.20.1's NativeImage stores ABGR; the codec's pixels are ARGB. */
    private static int abgr(int argb) {
        return argb & 0xFF00FF00 | (argb & 0xFF) << 16 | argb >> 16 & 0xFF;
    }

    /**
     * Whether the server said it has no picture for this photo (it faded, or the world lost it). Safe off the render
     * thread (tooltips may be built elsewhere, e.g. for recipe viewer search).
     */
    public static boolean faded(UUID id) {
        return FADED.contains(id);
    }

    /** Frees the least recently drawn entry, unless it was drawn this tick or the last. */
    private static boolean evictOne() {
        Iterator<Map.Entry<UUID, Entry>> it = ENTRIES.entrySet().iterator();
        if (!it.hasNext()) {
            return true;
        }
        Entry eldest = it.next().getValue();
        if (ticks - eldest.drawnAt <= 1) {
            return false;
        }
        if (eldest.texture != null) {
            Minecraft.getInstance().getTextureManager().release(eldest.texture);
        }
        it.remove();
        return true;
    }

    /** Leaving a world: pictures belong to that server. */
    public static void clear() {
        for (Entry entry : ENTRIES.values()) {
            if (entry.texture != null) {
                Minecraft.getInstance().getTextureManager().release(entry.texture);
            }
        }
        ENTRIES.clear();
        FADED.clear();
    }
}
