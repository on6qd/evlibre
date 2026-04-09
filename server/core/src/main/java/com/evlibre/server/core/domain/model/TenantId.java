package com.evlibre.server.core.domain.model;

import java.util.Objects;

public record TenantId(String value) {

    public TenantId {
        Objects.requireNonNull(value, "Tenant ID must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Tenant ID must not be blank");
        }
    }
}
