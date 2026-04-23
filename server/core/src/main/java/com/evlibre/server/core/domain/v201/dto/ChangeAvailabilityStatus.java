package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code ChangeAvailabilityStatusEnumType} — station's decision on a
 * {@code ChangeAvailability} request. {@link #SCHEDULED} indicates the station
 * has accepted the change but will defer it until any in-progress transaction
 * on the targeted EVSE/connector finishes (G03.FR.05, G04.FR.06).
 */
public enum ChangeAvailabilityStatus {
    ACCEPTED,
    REJECTED,
    SCHEDULED
}
