package com.evlibre.server.core.domain.v201.model;

/**
 * OCPP 2.0.1 {@code IdTokenEnumType} — the kind of identifier carried in an
 * {@link IdToken}. Used by {@code RequestStartTransaction}, {@code Authorize},
 * {@code TransactionEvent}, {@code SendLocalList}, and other flows that
 * reference a driver or vehicle identity.
 */
public enum IdTokenType {
    CENTRAL,
    EMAID,
    ISO14443,
    ISO15693,
    KEY_CODE,
    LOCAL,
    MAC_ADDRESS,
    NO_AUTHORIZATION
}
