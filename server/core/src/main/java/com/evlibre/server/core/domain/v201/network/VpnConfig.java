package com.evlibre.server.core.domain.v201.network;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code VPNType}: VPN settings attached to a
 * {@link NetworkConnectionProfile}. All fields except {@code group} are
 * required per the spec. Caps match the wire schema (server 512, user 20,
 * password 20, group 20, key 255).
 */
public record VpnConfig(String server,
                         String user,
                         String group,
                         String password,
                         String key,
                         VpnType type) {

    private static final int SERVER_MAX = 512;
    private static final int USER_MAX = 20;
    private static final int GROUP_MAX = 20;
    private static final int PASSWORD_MAX = 20;
    private static final int KEY_MAX = 255;

    public VpnConfig {
        Objects.requireNonNull(server, "VpnConfig.server must not be null");
        if (server.isBlank()) {
            throw new IllegalArgumentException("VpnConfig.server must not be blank");
        }
        if (server.length() > SERVER_MAX) {
            throw new IllegalArgumentException(
                    "VpnConfig.server must be <= " + SERVER_MAX + " chars, got " + server.length());
        }
        Objects.requireNonNull(user, "VpnConfig.user must not be null");
        if (user.length() > USER_MAX) {
            throw new IllegalArgumentException(
                    "VpnConfig.user must be <= " + USER_MAX + " chars, got " + user.length());
        }
        if (group != null && group.length() > GROUP_MAX) {
            throw new IllegalArgumentException(
                    "VpnConfig.group must be <= " + GROUP_MAX + " chars, got " + group.length());
        }
        Objects.requireNonNull(password, "VpnConfig.password must not be null");
        if (password.length() > PASSWORD_MAX) {
            throw new IllegalArgumentException(
                    "VpnConfig.password must be <= " + PASSWORD_MAX + " chars, got " + password.length());
        }
        Objects.requireNonNull(key, "VpnConfig.key must not be null");
        if (key.length() > KEY_MAX) {
            throw new IllegalArgumentException(
                    "VpnConfig.key must be <= " + KEY_MAX + " chars, got " + key.length());
        }
        Objects.requireNonNull(type, "VpnConfig.type must not be null");
    }
}
