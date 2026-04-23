package com.evlibre.server.core.domain.v201.network;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code APNType}: cellular/gateway credentials attached to a
 * {@link NetworkConnectionProfile}. Only the address and auth method are
 * required; everything else is optional.
 *
 * <p>Field-length caps mirror the spec ({@code apn} 512, user/password 20,
 * preferredNetwork 6) so violations fail at the domain boundary rather than
 * at the JSON schema validator.
 */
public record ApnConfig(String apn,
                         String apnUserName,
                         String apnPassword,
                         Integer simPin,
                         String preferredNetwork,
                         Boolean useOnlyPreferredNetwork,
                         ApnAuthMethod apnAuthentication) {

    private static final int APN_MAX = 512;
    private static final int USER_MAX = 20;
    private static final int PASSWORD_MAX = 20;
    private static final int PREFERRED_NETWORK_MAX = 6;

    public ApnConfig {
        Objects.requireNonNull(apn, "ApnConfig.apn must not be null");
        if (apn.isBlank()) {
            throw new IllegalArgumentException("ApnConfig.apn must not be blank");
        }
        if (apn.length() > APN_MAX) {
            throw new IllegalArgumentException(
                    "ApnConfig.apn must be <= " + APN_MAX + " chars, got " + apn.length());
        }
        if (apnUserName != null && apnUserName.length() > USER_MAX) {
            throw new IllegalArgumentException(
                    "ApnConfig.apnUserName must be <= " + USER_MAX + " chars, got " + apnUserName.length());
        }
        if (apnPassword != null && apnPassword.length() > PASSWORD_MAX) {
            throw new IllegalArgumentException(
                    "ApnConfig.apnPassword must be <= " + PASSWORD_MAX + " chars, got " + apnPassword.length());
        }
        if (preferredNetwork != null && preferredNetwork.length() > PREFERRED_NETWORK_MAX) {
            throw new IllegalArgumentException(
                    "ApnConfig.preferredNetwork must be <= " + PREFERRED_NETWORK_MAX
                            + " chars, got " + preferredNetwork.length());
        }
        Objects.requireNonNull(apnAuthentication, "ApnConfig.apnAuthentication must not be null");
    }
}
