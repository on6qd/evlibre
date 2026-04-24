package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code GetCertificateStatusEnumType}. Outcome of the CSMS's
 * attempt to relay the station's OCSP query to the issuing CA.
 */
public enum GetCertificateStatus {
    ACCEPTED,
    FAILED
}
