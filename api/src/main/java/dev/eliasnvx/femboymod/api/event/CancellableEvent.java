package dev.eliasnvx.femboymod.api.event;

/**
 * An event whose default action can be prevented.
 *
 * <p>Once a listener cancels the event, listeners with lower priority are <b>not</b> called and
 * the action is skipped by the poster.
 */
public interface CancellableEvent extends FemboyEvent {

    /**
     * Returns whether the event has been cancelled.
     *
     * @return {@code true} if cancelled
     */
    boolean isCancelled();

    /** Cancels the event. Cancellation cannot be undone. */
    void cancel();
}
