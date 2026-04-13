package com.evlibre.server.test.fakes;

import com.evlibre.server.core.domain.ports.outbound.TimeProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class FakeTimeProvider implements TimeProvider {

    private final AtomicReference<Instant> clock;

    public FakeTimeProvider() {
        this(Instant.parse("2025-01-15T10:00:00Z"));
    }

    public FakeTimeProvider(Instant fixedTime) {
        this.clock = new AtomicReference<>(fixedTime);
    }

    @Override
    public Instant now() {
        return clock.get();
    }

    public void set(Instant instant) {
        clock.set(instant);
    }

    public void advance(Duration duration) {
        clock.updateAndGet(current -> current.plus(duration));
    }
}
