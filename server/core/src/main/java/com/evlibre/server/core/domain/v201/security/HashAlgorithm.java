package com.evlibre.server.core.domain.v201.security;

/**
 * OCPP 2.0.1 {@code HashAlgorithmEnumType}. Used inside
 * {@link CertificateHashData} to identify the hash used over the issuer
 * certificate's Name / public key.
 */
public enum HashAlgorithm {
    SHA256,
    SHA384,
    SHA512
}
