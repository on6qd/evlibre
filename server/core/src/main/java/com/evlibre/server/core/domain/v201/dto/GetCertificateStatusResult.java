package com.evlibre.server.core.domain.v201.dto;

import java.util.Objects;

/**
 * Typed result of a {@code GetCertificateStatus} response (block A04).
 * {@link #ocspResult} carries the DER-encoded / base64-wrapped OCSP response
 * from the CA. Spec allows the field to be omitted when {@link #status} is
 * {@link GetCertificateStatus#FAILED}.
 */
public record GetCertificateStatusResult(
        GetCertificateStatus status,
        String ocspResult,
        String statusInfoReason) {

    public GetCertificateStatusResult {
        Objects.requireNonNull(status, "status");
        if (ocspResult != null && ocspResult.length() > 5500) {
            throw new IllegalArgumentException(
                    "ocspResult exceeds 5500 char limit (" + ocspResult.length() + ")");
        }
    }

    public static GetCertificateStatusResult accepted(String ocspResult) {
        return new GetCertificateStatusResult(GetCertificateStatus.ACCEPTED, ocspResult, null);
    }

    public static GetCertificateStatusResult failed(String reasonCode) {
        return new GetCertificateStatusResult(GetCertificateStatus.FAILED, null, reasonCode);
    }

    public boolean isAccepted() {
        return status == GetCertificateStatus.ACCEPTED;
    }
}
