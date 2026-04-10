package com.evlibre.server.core.domain.dto;

import com.evlibre.server.core.domain.model.AuthorizationStatus;

public record AuthorizationResult(
        AuthorizationStatus status,
        String idTag
) {}
