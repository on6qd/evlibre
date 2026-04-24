package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code DeleteCertificateStatusEnumType}.
 * {@code NotFound} when the station has no certificate matching the hash
 * tuple; {@code Failed} when deletion failed for some other reason (e.g.
 * the cert is currently in use).
 */
public enum DeleteCertificateStatus {
    ACCEPTED,
    FAILED,
    NOT_FOUND
}
