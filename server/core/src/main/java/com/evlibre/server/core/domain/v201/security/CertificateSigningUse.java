package com.evlibre.server.core.domain.v201.security;

/**
 * OCPP 2.0.1 {@code CertificateSigningUseEnumType} — tells the CSMS whether
 * a CSR is for the station's own CSMS-facing certificate or for an ISO 15118
 * (V2G) contract certificate.
 */
public enum CertificateSigningUse {
    CHARGING_STATION_CERTIFICATE,
    V2G_CERTIFICATE
}
