package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code OperationalStatusEnumType} — the availability change
 * requested in a {@code ChangeAvailability} call. Distinct from v1.6
 * {@code AvailabilityType} both in name and in that v2.0.1 applies it at
 * station / EVSE / connector granularity via a nested EVSE locator.
 */
public enum OperationalStatus {
    INOPERATIVE,
    OPERATIVE
}
