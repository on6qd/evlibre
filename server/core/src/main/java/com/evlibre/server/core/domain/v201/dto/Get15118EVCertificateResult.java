package com.evlibre.server.core.domain.v201.dto;

import java.util.Objects;

/**
 * Typed result of a {@code Get15118EVCertificate} response (block M06).
 * The {@code exiResponse} is mandatory on the wire per the spec — the
 * station needs something to hand back to the EV, even when the exchange
 * ultimately fails (the EXI payload in that case carries the ResponseCode
 * specified by ISO 15118).
 */
public record Get15118EVCertificateResult(
        Iso15118EVCertificateStatus status,
        String exiResponse,
        String statusInfoReason) {

    public Get15118EVCertificateResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(exiResponse, "exiResponse");
        if (exiResponse.length() > 5600) {
            throw new IllegalArgumentException(
                    "exiResponse exceeds 5600 char limit (" + exiResponse.length() + ")");
        }
    }

    public static Get15118EVCertificateResult accepted(String exiResponse) {
        return new Get15118EVCertificateResult(
                Iso15118EVCertificateStatus.ACCEPTED, exiResponse, null);
    }

    public static Get15118EVCertificateResult failed(String exiResponse, String reasonCode) {
        return new Get15118EVCertificateResult(
                Iso15118EVCertificateStatus.FAILED, exiResponse, reasonCode);
    }

    public boolean isAccepted() {
        return status == Iso15118EVCertificateStatus.ACCEPTED;
    }
}
