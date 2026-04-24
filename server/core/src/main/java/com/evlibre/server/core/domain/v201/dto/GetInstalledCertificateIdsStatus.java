package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code GetInstalledCertificateStatusEnumType}. {@code NotFound}
 * means the station found no certificates matching the request's filter — in
 * that case the station returns an empty chain.
 */
public enum GetInstalledCertificateIdsStatus {
    ACCEPTED,
    NOT_FOUND
}
