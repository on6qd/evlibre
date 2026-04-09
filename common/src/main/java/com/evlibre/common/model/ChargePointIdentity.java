package com.evlibre.common.model;

import java.util.Objects;

public record ChargePointIdentity(String value) {

    public ChargePointIdentity {
        Objects.requireNonNull(value, "Charge point identity must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Charge point identity must not be blank");
        }
        if (value.length() > 48) {
            throw new IllegalArgumentException("Charge point identity must not exceed 48 characters");
        }
    }
}
