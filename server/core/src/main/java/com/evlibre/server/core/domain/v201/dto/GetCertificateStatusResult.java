package com.evlibre.server.core.domain.v201.dto;

import java.util.Objects;

/**
 * Typed result of a {@code GetCertificateStatus} response (block A04).
 * {@link #ocspResult} carries the DER-encoded / base64-wrapped OCSP response
 * from the CA. Per the OCPP 2.0.1 schema description
 * ({@code GetCertificateStatusResponse.ocspResult}: "MAY only be omitted
 * when status is not Accepted"), the contract is enforced here:
 * {@code ocspResult} MUST be non-blank when {@link #status} is
 * {@link GetCertificateStatus#ACCEPTED}, and is typically absent for
 * {@link GetCertificateStatus#FAILED}.
 */
public record GetCertificateStatusResult(
        GetCertificateStatus status,
        String ocspResult,
        String statusInfoReason) {

    public GetCertificateStatusResult {
        Objects.requireNonNull(status, "status");
        if (status == GetCertificateStatus.ACCEPTED) {
            Objects.requireNonNull(ocspResult, "ocspResult is required when status=Accepted");
            if (ocspResult.isBlank()) {
                throw new IllegalArgumentException(
                        "ocspResult must not be blank when status=Accepted");
            }
        }
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
