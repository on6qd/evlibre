package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code InstallCertificateStatusEnumType} — outcome of an
 * {@code InstallCertificate} request.
 *
 * <ul>
 *   <li>{@link #ACCEPTED} — station verified and stored the certificate.</li>
 *   <li>{@link #REJECTED} — station verified the certificate but refuses it
 *       (e.g. policy violation).</li>
 *   <li>{@link #FAILED} — verification failed (bad PEM, chain invalid).</li>
 * </ul>
 */
public enum InstallCertificateStatus {
    ACCEPTED,
    REJECTED,
    FAILED
}
