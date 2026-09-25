package dev.eliasnvx.femboymod.event;

import dev.eliasnvx.femboymod.api.event.CancellableEvent;
import dev.eliasnvx.femboymod.api.event.EventPriority;
import dev.eliasnvx.femboymod.api.event.FemboyEvent;
import dev.eliasnvx.femboymod.api.event.FemboyEventBus;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class FemboyEventBusImpl implements FemboyEventBus {

    private static final Comparator<Listener> ORDER = Comparator.comparingInt(l -> l.priority().ordinal());

    private final Logger logger;
    /** Snapshot lists (copy-on-write): posting never locks and never allocates. */
    private final Map<Class<?>, List<Listener>> listeners = new ConcurrentHashMap<>();

    public FemboyEventBusImpl(Logger logger) {
        this.logger = logger;
    }

    @Override
    public <E extends FemboyEvent> void addListener(Class<E> type, EventPriority priority, Consumer<? super E> listener) {
        Listener entry = new Listener(priority, listener);
        listeners.compute(type, (key, current) -> {
            List<Listener> next = current == null ? new ArrayList<>(1) : new ArrayList<>(current);
            next.add(entry);
            next.sort(ORDER); // stable: same priority keeps registration order
            return List.copyOf(next);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends FemboyEvent> E post(E event) {
        List<Listener> list = listeners.get(event.getClass());
        if (list == null) {
            return event;
        }
        for (int i = 0; i < list.size(); i++) {
            if (event instanceof CancellableEvent cancellable && cancellable.isCancelled()) {
                break;
            }
            Listener listener = list.get(i);
            try {
                ((Consumer<E>) listener.consumer()).accept(event);
            } catch (Throwable t) {
                logger.error("Listener for {} threw an exception", event.getClass().getName(), t);
            }
        }
        return event;
    }

    private record Listener(EventPriority priority, Consumer<?> consumer) {
    }
}
