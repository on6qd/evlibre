package com.evlibre.server.core.domain.v201.model;

/**
 * OCPP 2.0.1 {@code AuthorizationStatusEnumType}. Distinct from v1.6's
 * 4-value {@code AuthorizationStatus} (Accepted/Blocked/Expired/Invalid +
 * ConcurrentTx); v2.0.1 adds five new values that let the station express
 * non-binary outcomes — {@link #NO_CREDIT} for prepayment exhaustion,
 * {@link #NOT_ALLOWED_TYPE_EVSE} when the EVSE type is wrong for this
 * token (e.g. AC vs DC), {@link #NOT_AT_THIS_LOCATION} when geofencing
 * applies, {@link #NOT_AT_THIS_TIME} for time-of-day restrictions, and
 * {@link #UNKNOWN} as a catch-all "we don't recognise this id".
 */
public enum AuthorizationStatus {
    ACCEPTED,
    BLOCKED,
    CONCURRENT_TX,
    EXPIRED,
    INVALID,
    NO_CREDIT,
    NOT_ALLOWED_TYPE_EVSE,
    NOT_AT_THIS_LOCATION,
    NOT_AT_THIS_TIME,
    UNKNOWN
}
