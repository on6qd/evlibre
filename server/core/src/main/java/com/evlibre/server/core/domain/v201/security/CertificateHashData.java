package com.evlibre.server.core.domain.v201.security;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code CertificateHashDataType} — the tuple used to identify a
 * certificate without transmitting the full DER/PEM. All four fields together
 * uniquely identify the certificate on the station's trust store.
 *
 * <p>Field sizes are the spec maximums: 128 chars for the issuer-name and
 * issuer-key hash hex strings (big enough for SHA-512), 40 chars for the
 * serial number.
 */
public record CertificateHashData(
        HashAlgorithm hashAlgorithm,
        String issuerNameHash,
        String issuerKeyHash,
        String serialNumber) {

    public CertificateHashData {
        Objects.requireNonNull(hashAlgorithm, "hashAlgorithm");
        Objects.requireNonNull(issuerNameHash, "issuerNameHash");
        Objects.requireNonNull(issuerKeyHash, "issuerKeyHash");
        Objects.requireNonNull(serialNumber, "serialNumber");
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
    }
}
