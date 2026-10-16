package dev.eliasnvx.femboymod.compat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Slots of other mods (Curios, Trinkets, Accessories; SPEC §4.3) that may hold our cosmetics. Each installed slot
 * mod registers a source from its {@code compat.<modid>} package; {@code CosmeticsManager#worn} merges what they
 * hold into the outfit, so a cosmetic counts and renders the same in their slot as in ours.
 */
public final class ExternalSlots {

    /** Lists every stack a player has in one slot mod's slots (cosmetic or not; empty stacks are fine). */
    @FunctionalInterface
    public interface Source {
        void forEach(Player player, Consumer<ItemStack> action);
    }

    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();

    private ExternalSlots() {
    }

    public static void register(Source source) {
        SOURCES.add(source);
    }

    /** For GameTests. */
    public static void unregister(Source source) {
        SOURCES.remove(source);
    }

    public static boolean any() {
        return !SOURCES.isEmpty();
    }

    public static void forEach(Player player, Consumer<ItemStack> action) {
        for (Source source : SOURCES) {
            source.forEach(player, action);
        }
    }
}
