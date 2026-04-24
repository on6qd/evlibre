package com.evlibre.server.core.domain.v201.dto;

import com.evlibre.server.core.domain.v201.model.IdToken;
import com.evlibre.server.core.domain.v201.security.CertificateHashData;

/**
 * OCPP 2.0.1 {@code CustomerInformationRequest} identifier bundle — the three
 * mutually-optional fields the CSMS may use to pick out a customer:
 * {@code customerIdentifier} (free-form string, maxLength 64),
 * {@code idToken}, or {@code customerCertificate}.
 *
 * <p>The spec says "one of the possible identifiers should be in the request
 * message" (SHOULD, not MUST), so we allow all three to be absent for
 * operator-level queries that target no specific customer. All three may
 * also be present in a single call.
 */
public record CustomerInformationTarget(String customerIdentifier,
                                         IdToken idToken,
                                         CertificateHashData certificate) {

    private static final int CUSTOMER_IDENTIFIER_MAX = 64;

    public CustomerInformationTarget {
        if (customerIdentifier != null && customerIdentifier.length() > CUSTOMER_IDENTIFIER_MAX) {
            throw new IllegalArgumentException(
                    "customerIdentifier must be <= " + CUSTOMER_IDENTIFIER_MAX
                            + " chars, got " + customerIdentifier.length());
        }
    }

    public boolean isEmpty() {
        return customerIdentifier == null && idToken == null && certificate == null;
    }

    public static CustomerInformationTarget none() {
        return new CustomerInformationTarget(null, null, null);
    }

    public static CustomerInformationTarget byIdentifier(String customerIdentifier) {
        return new CustomerInformationTarget(customerIdentifier, null, null);
    }

    public static CustomerInformationTarget byIdToken(IdToken idToken) {
        return new CustomerInformationTarget(null, idToken, null);
    }

    public static CustomerInformationTarget byCertificate(CertificateHashData certificate) {
        return new CustomerInformationTarget(null, null, certificate);
    }
}
