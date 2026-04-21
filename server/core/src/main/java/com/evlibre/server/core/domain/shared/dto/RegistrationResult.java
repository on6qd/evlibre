package com.evlibre.server.core.domain.shared.dto;

import com.evlibre.server.core.domain.shared.model.RegistrationStatus;

import java.time.Instant;

public record RegistrationResult(
        RegistrationStatus status,
        Instant currentTime,
        int heartbeatInterval
) {}
