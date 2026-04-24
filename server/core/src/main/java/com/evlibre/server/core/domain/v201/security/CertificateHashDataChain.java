package com.evlibre.server.core.domain.v201.security;

import java.util.List;
import java.util.Objects;

/**
 * OCPP 2.0.1 {@code CertificateHashDataChainType} — one trust chain entry in
 * a {@code GetInstalledCertificateIdsResponse}. The leaf is
 * {@link #certificateHashData}; {@link #childCertificateHashData} lists the
 * child certificates derived from that trust root (used for V2G chains).
 *
 * <p>Per the spec schema, {@code childCertificateHashData} has
 * {@code maxItems: 4}. Enforced here so the invariant is caught at
 * construction rather than only by the wire validator.
 */
public record CertificateHashDataChain(
        GetCertificateIdUse certificateType,
        CertificateHashData certificateHashData,
        List<CertificateHashData> childCertificateHashData) {

    private static final int MAX_CHILDREN = 4;

    public CertificateHashDataChain {
        Objects.requireNonNull(certificateType, "certificateType");
        Objects.requireNonNull(certificateHashData, "certificateHashData");
        childCertificateHashData = childCertificateHashData == null
                ? List.of()
                : List.copyOf(childCertificateHashData);
        if (childCertificateHashData.size() > MAX_CHILDREN) {
            throw new IllegalArgumentException(
                    "childCertificateHashData exceeds maxItems "
                            + MAX_CHILDREN + " (" + childCertificateHashData.size() + ")");
        }
    }

    public static CertificateHashDataChain of(
            GetCertificateIdUse certificateType, CertificateHashData certificateHashData) {
        return new CertificateHashDataChain(certificateType, certificateHashData, List.of());
    }
}
