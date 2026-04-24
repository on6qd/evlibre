package com.evlibre.server.core.domain.v201.security;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code OCSPRequestDataType} — the station's OCSP query relayed
 * via {@code GetCertificateStatus} (block A04). Carries the four hash fields
 * shared with {@link CertificateHashData} plus the OCSP responder URL the
 * station wants the CSMS to hit.
 *
 * <p>Field sizes are the spec maximums: 128 chars for the issuer-name and
 * issuer-key hash hex strings, 40 chars for the serial number, 512 chars for
 * the responderURL.
 */
public record OcspRequestData(
        HashAlgorithm hashAlgorithm,
        String issuerNameHash,
        String issuerKeyHash,
        String serialNumber,
        String responderURL) {

    public OcspRequestData {
        Objects.requireNonNull(hashAlgorithm, "hashAlgorithm");
        Objects.requireNonNull(issuerNameHash, "issuerNameHash");
        Objects.requireNonNull(issuerKeyHash, "issuerKeyHash");
        Objects.requireNonNull(serialNumber, "serialNumber");
        Objects.requireNonNull(responderURL, "responderURL");
        if (issuerNameHash.length() > 128) {
            throw new IllegalArgumentException(
                    "issuerNameHash exceeds 128 char limit (" + issuerNameHash.length() + ")");
        }
        if (issuerKeyHash.length() > 128) {
            throw new IllegalArgumentException(
                    "issuerKeyHash exceeds 128 char limit (" + issuerKeyHash.length() + ")");
        }
        if (serialNumber.length() > 40) {
            throw new IllegalArgumentException(
                    "serialNumber exceeds 40 char limit (" + serialNumber.length() + ")");
        }
        if (responderURL.length() > 512) {
            throw new IllegalArgumentException(
                    "responderURL exceeds 512 char limit (" + responderURL.length() + ")");
        }
    }
}
