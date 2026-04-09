package com.evlibre.server.core.domain.ports.outbound;

import java.time.Instant;

public interface TimeProvider {

    Instant now();
}
