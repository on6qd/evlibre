package com.evlibre.server.core.domain.dto;

import com.evlibre.server.core.domain.model.RegistrationStatus;

import java.time.Instant;

public record RegistrationResult(
        RegistrationStatus status,
        Instant currentTime,
        int heartbeatInterval
) {}
