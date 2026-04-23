package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code SendLocalListStatusEnumType}. {@link #VERSION_MISMATCH}
 * is returned by the station when a {@code Differential} update arrives
 * with a {@code versionNumber} that is less than or equal to the station's
 * current list version (D01.FR.19).
 */
public enum SendLocalListStatus {
    ACCEPTED,
    FAILED,
    VERSION_MISMATCH
}
