package com.evlibre.server.core.domain.v201.dto;

import java.util.Objects;

/**
 * Typed result of a {@code DeleteCertificate} call (block M04).
 */
public record DeleteCertificateResult(
        DeleteCertificateStatus status,
        String statusInfoReason) {

    public DeleteCertificateResult {
        Objects.requireNonNull(status, "status");
    }

    public boolean isAccepted() {
        return status == DeleteCertificateStatus.ACCEPTED;
    }
}
