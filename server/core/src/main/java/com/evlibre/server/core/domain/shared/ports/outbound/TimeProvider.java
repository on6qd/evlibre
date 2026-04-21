package com.evlibre.server.core.domain.shared.ports.outbound;

import java.time.Instant;

public interface TimeProvider {

    Instant now();
}
