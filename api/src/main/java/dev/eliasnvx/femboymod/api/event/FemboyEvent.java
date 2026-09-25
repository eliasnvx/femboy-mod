package dev.eliasnvx.femboymod.api.event;

/**
 * Marker for events posted on the {@link FemboyEventBus}.
 *
 * <p>Events are dispatched by their exact runtime class: a listener registered for a superclass
 * does not receive subclass events.
 */
public interface FemboyEvent {
}
