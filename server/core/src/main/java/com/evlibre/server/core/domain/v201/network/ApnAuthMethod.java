package com.evlibre.server.core.domain.v201.network;

/**
 * OCPP 2.0.1 {@code APNAuthenticationEnumType}: authentication method for a
 * cellular {@link ApnConfig}. Wire form is {@code CHAP}, {@code NONE},
 * {@code PAP}, {@code AUTO} — already UPPER_CASE, so no mapping trickery.
 */
public enum ApnAuthMethod {
    CHAP,
    NONE,
    PAP,
    AUTO
}
