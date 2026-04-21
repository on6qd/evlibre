package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.server.core.domain.shared.ports.outbound.TimeProvider;

import java.time.Instant;

public class SystemTimeProvider implements TimeProvider {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
