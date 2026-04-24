package com.evlibre.server.core.domain.v201.security;

/**
 * OCPP 2.0.1 {@code InstallCertificateUseEnumType} — which trust root is
 * being installed by the CSMS. Note this is a strict subset of
 * {@link GetCertificateIdUse}: {@code V2GCertificateChain} is discoverable
 * on the station but never directly installed through {@code InstallCertificate}.
 */
public enum InstallCertificateUse {
    V2G_ROOT_CERTIFICATE,
    MO_ROOT_CERTIFICATE,
    CSMS_ROOT_CERTIFICATE,
    MANUFACTURER_ROOT_CERTIFICATE
}
