package com.evlibre.server.core.domain.v201.dto;

import com.evlibre.server.core.domain.v201.security.CertificateHashDataChain;

import java.util.List;
import java.util.Objects;

/**
 * Typed result of a {@code GetInstalledCertificateIds} call (block M03).
 * When {@link #status} is {@link GetInstalledCertificateIdsStatus#ACCEPTED}
 * the station returned at least one chain; when {@code NOT_FOUND} the chain
 * list is empty.
 */
public record GetInstalledCertificateIdsResult(
        GetInstalledCertificateIdsStatus status,
        List<CertificateHashDataChain> certificateHashDataChain,
        String statusInfoReason) {

    public GetInstalledCertificateIdsResult {
        Objects.requireNonNull(status, "status");
        certificateHashDataChain = certificateHashDataChain == null
                ? List.of()
                : List.copyOf(certificateHashDataChain);
    }

    public boolean isAccepted() {
        return status == GetInstalledCertificateIdsStatus.ACCEPTED;
    }
}
