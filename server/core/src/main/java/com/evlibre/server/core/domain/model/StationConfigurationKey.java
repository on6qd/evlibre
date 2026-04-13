package com.evlibre.server.core.domain.model;

public record StationConfigurationKey(
        String key,
        String value,
        boolean readonly
) {}
