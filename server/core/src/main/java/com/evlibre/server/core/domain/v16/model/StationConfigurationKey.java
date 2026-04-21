package com.evlibre.server.core.domain.v16.model;

public record StationConfigurationKey(
        String key,
        String value,
        boolean readonly
) {}
