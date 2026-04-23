package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code ClearCacheStatusEnumType}. Station returns {@link #REJECTED}
 * either when {@code AuthCacheEnabled} is {@code false} (C11.FR.04) or when it
 * technically failed to clear the cache (C11.FR.05) — the {@code statusInfo}
 * reasonCode disambiguates.
 */
public enum ClearCacheStatus {
    ACCEPTED,
    REJECTED
}
