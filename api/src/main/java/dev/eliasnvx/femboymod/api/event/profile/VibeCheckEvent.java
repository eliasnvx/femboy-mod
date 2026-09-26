package dev.eliasnvx.femboymod.api.event.profile;

import dev.eliasnvx.femboymod.api.event.FemboyEvent;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

/**
 * Posted on the logical server when a Vibe Check Scanner has rated a player, before the score is shown and put on
 * the server leaderboard. Listeners may change the score (clamped to 0..100 afterwards). Not cancellable.
 */
@ApiStatus.AvailableSince("0.1.0")
public final class VibeCheckEvent implements FemboyEvent {

    private final ServerPlayer player;
    private int score;

    /**
     * @param player the scanned player
     * @param score  the computed vibe, 0..100
     */
    public VibeCheckEvent(ServerPlayer player, int score) {
        this.player = player;
        this.score = score;
    }

    /** @return the scanned player */
    public ServerPlayer player() {
        return player;
    }

    /** @return the vibe, 0..100 */
    public int score() {
        return score;
    }

    /** @param score the new vibe (clamped to 0..100) */
    public void setScore(int score) {
        this.score = score;
    }
}
