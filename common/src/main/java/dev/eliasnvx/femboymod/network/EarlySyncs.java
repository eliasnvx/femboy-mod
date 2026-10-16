package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.entity.player.Player;

/**
 * Syncs the client gets before its player exists. On 1.20.1 Fabric the server's join sync (outfit, profile) arrives
 * before the login packet is handled, and the packet context keeps the (missing) player it saw on arrival, so without
 * this the client lost its own outfit on every join. They are kept and applied on the first client tick with a local
 * player ({@code FemboyModClient}), and dropped when leaving the world. Client thread only.
 */
public final class EarlySyncs {

    private static final List<Consumer<Player>> PENDING = new ArrayList<>();

    private EarlySyncs() {
    }

    /** Runs the action with the client player: now, or as soon as the player exists. Call on the client thread. */
    public static void withPlayer(NetworkManager.PacketContext context, Consumer<Player> action) {
        Player player = context.getPlayer();
        if (player != null) {
            action.accept(player);
        } else {
            PENDING.add(action);
        }
    }

    /** Applies what arrived early, in order; called every client tick while there is a local player. */
    public static void flush(Player player) {
        if (PENDING.isEmpty()) {
            return;
        }
        List<Consumer<Player>> pending = List.copyOf(PENDING);
        PENDING.clear();
        pending.forEach(action -> action.accept(player));
    }

    /** Forgets syncs for a world that was left before the player appeared. */
    public static void clear() {
        PENDING.clear();
    }
}
