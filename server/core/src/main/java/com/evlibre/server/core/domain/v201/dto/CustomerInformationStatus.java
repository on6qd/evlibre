package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code CustomerInformationStatusEnumType} — outcome of a
 * {@code CustomerInformation} request.
 *
 * <p>{@code Invalid} is distinct from {@code Rejected}: the station tells the
 * CSMS its customer-identifier format was not recognisable (e.g. unsupported
 * IdToken type), as opposed to a general refusal.
 */
public enum CustomerInformationStatus {
    ACCEPTED,
    REJECTED,
    INVALID
}
