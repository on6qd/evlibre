package com.evlibre.server.core.domain.v201.security;

/**
 * OCPP 2.0.1 {@code GetCertificateIdUseEnumType} — tags each entry returned
 * by a {@code GetInstalledCertificateIds} / filters the request. The list is
 * a superset of {@link InstallCertificateUse} because
 * {@code V2GCertificateChain} is discoverable on the station but never
 * directly installed by the CSMS.
 */
public enum GetCertificateIdUse {
    V2G_ROOT_CERTIFICATE,
    MO_ROOT_CERTIFICATE,
    CSMS_ROOT_CERTIFICATE,
    V2G_CERTIFICATE_CHAIN,
    MANUFACTURER_ROOT_CERTIFICATE
}
