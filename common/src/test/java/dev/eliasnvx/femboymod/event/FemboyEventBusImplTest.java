package dev.eliasnvx.femboymod.event;

import dev.eliasnvx.femboymod.api.event.CancellableEvent;
import dev.eliasnvx.femboymod.api.event.EventPriority;
import dev.eliasnvx.femboymod.api.event.FemboyEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FemboyEventBusImplTest {

    static final class Ping implements FemboyEvent {
        final List<String> calls = new ArrayList<>();
    }

    static final class Stoppable implements CancellableEvent {
        final List<String> calls = new ArrayList<>();
        private boolean cancelled;

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

    private final FemboyEventBusImpl bus = new FemboyEventBusImpl(NOPLogger.NOP_LOGGER);

    @Test
    void listenersRunByPriorityThenRegistrationOrder() {
        bus.addListener(Ping.class, EventPriority.LOW, e -> e.calls.add("low"));
        bus.addListener(Ping.class, e -> e.calls.add("normal1"));
        bus.addListener(Ping.class, EventPriority.HIGHEST, e -> e.calls.add("highest"));
        bus.addListener(Ping.class, e -> e.calls.add("normal2"));

        assertEquals(List.of("highest", "normal1", "normal2", "low"), bus.post(new Ping()).calls);
    }

    @Test
    void cancellationStopsLowerPriorityListeners() {
        bus.addListener(Stoppable.class, EventPriority.HIGH, e -> {
            e.calls.add("high");
            e.cancel();
        });
        bus.addListener(Stoppable.class, e -> e.calls.add("normal"));

        Stoppable event = bus.post(new Stoppable());
        assertTrue(event.isCancelled());
        assertEquals(List.of("high"), event.calls);
    }

    @Test
    void throwingListenerDoesNotBreakOthers() {
        bus.addListener(Ping.class, e -> {
            throw new RuntimeException("boom");
        });
        bus.addListener(Ping.class, e -> e.calls.add("after"));

        assertEquals(List.of("after"), bus.post(new Ping()).calls);
    }

    @Test
    void eventsDispatchByExactClassOnly() {
        bus.addListener(Ping.class, e -> e.calls.add("ping"));
        assertTrue(bus.post(new Stoppable()).calls.isEmpty());
    }
}
