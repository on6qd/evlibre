package com.evlibre.server.core.domain.v201.dto;

import java.util.Objects;

/**
 * Typed result of an {@code InstallCertificate} call (block M01). Only
 * {@link InstallCertificateStatus#ACCEPTED} counts as success;
 * {@code REJECTED} means policy-denied and {@code FAILED} means the chain
 * didn't verify. {@link #statusInfoReason} carries the optional
 * {@code statusInfo.reasonCode} when the station included one.
 */
public record InstallCertificateResult(
        InstallCertificateStatus status,
        String statusInfoReason) {

    public InstallCertificateResult {
        Objects.requireNonNull(status, "status");
    }

    public boolean isAccepted() {
        return status == InstallCertificateStatus.ACCEPTED;
    }
}
