package com.evlibre.server.core.domain.v201.dto;

import java.util.Objects;

/**
 * CSMS-side decision for an incoming {@code SignCertificateRequest}
 * (block A02) — whether to accept the CSR for signing, plus an optional
 * {@code statusInfo.reasonCode} carried back to the station.
 *
 * <p>Outcome is {@link GenericStatus#ACCEPTED} when the CSMS has taken
 * custody of the CSR (the actual signed certificate is delivered later via
 * an outbound {@code CertificateSigned}), or {@link GenericStatus#REJECTED}
 * when the CSR itself is malformed or policy-denied.
 */
public record SignCertificateResult(
        GenericStatus status,
        String statusInfoReason) {

    public SignCertificateResult {
        Objects.requireNonNull(status, "status");
    }

    public static SignCertificateResult accepted() {
        return new SignCertificateResult(GenericStatus.ACCEPTED, null);
    }

    public static SignCertificateResult rejected(String reasonCode) {
        return new SignCertificateResult(GenericStatus.REJECTED, reasonCode);
    }

    public boolean isAccepted() {
        return status == GenericStatus.ACCEPTED;
    }
}
