package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code GetChargingProfileStatusEnumType} — outcome of the
 * immediate response to {@code GetChargingProfiles}. {@link #ACCEPTED} means
 * the actual profiles will arrive in one or more follow-up
 * {@code ReportChargingProfilesRequest} messages matching the request id;
 * {@link #NO_PROFILES} means nothing matched and no reports will follow.
 */
public enum GetChargingProfilesStatus {
    ACCEPTED,
    NO_PROFILES
}
