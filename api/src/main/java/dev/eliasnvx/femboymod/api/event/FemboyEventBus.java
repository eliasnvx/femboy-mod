package dev.eliasnvx.femboymod.api.event;

import java.util.function.Consumer;

/**
 * Loader-independent event bus used by Femboy Mod. Obtain it from
 * {@link dev.eliasnvx.femboymod.api.FemboyApi#events()}.
 *
 * <p>Listeners are called on the thread that posts the event (usually the server or client main
 * thread; see each event's documentation). A listener that throws is logged and skipped; the
 * remaining listeners still run.
 */
public interface FemboyEventBus {

    /**
     * Registers a listener with {@link EventPriority#NORMAL} priority.
     *
     * @param type     the exact event class to listen for
     * @param listener the listener
     * @param <E>      event type
     */
    default <E extends FemboyEvent> void addListener(Class<E> type, Consumer<? super E> listener) {
        addListener(type, EventPriority.NORMAL, listener);
    }

    /**
     * Registers a listener.
     *
     * @param type     the exact event class to listen for
     * @param priority call order relative to other listeners
     * @param listener the listener
     * @param <E>      event type
     */
    <E extends FemboyEvent> void addListener(Class<E> type, EventPriority priority, Consumer<? super E> listener);

    /**
     * Posts an event to all listeners registered for its exact class. Addons may post their own
     * event types as well.
     *
     * @param event the event
     * @param <E>   event type
     * @return the same event, for reading results such as cancellation
     */
    <E extends FemboyEvent> E post(E event);
}
