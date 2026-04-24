package com.evlibre.server.core.domain.v201.security;

/**
 * OCPP 2.0.1 {@code CertificateActionEnumType} — carried by
 * {@code Get15118EVCertificate} to tell the CSMS whether the EV is installing
 * a brand-new contract certificate or updating an existing one.
 */
public enum CertificateAction {
    INSTALL,
    UPDATE
}
