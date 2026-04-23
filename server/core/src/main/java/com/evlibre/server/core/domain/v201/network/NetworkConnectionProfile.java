package com.evlibre.server.core.domain.v201.network;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code NetworkConnectionProfileType}: the bundle of
 * communication parameters the station should use at a given configuration
 * slot. Passed as the {@code connectionData} payload of a
 * {@code SetNetworkProfileRequest}.
 *
 * <p>{@code apn} is only meaningful when {@link #ocppInterface} is one of the
 * {@code Wireless*} values; {@code vpn} is independent of the interface and
 * may be attached to a wired or wireless profile.
 */
public record NetworkConnectionProfile(OcppVersion ocppVersion,
                                        OcppTransport ocppTransport,
                                        OcppInterface ocppInterface,
                                        int messageTimeoutSeconds,
                                        int securityProfile,
                                        String ocppCsmsUrl,
                                        ApnConfig apn,
                                        VpnConfig vpn) {

    private static final int URL_MAX = 512;

    public NetworkConnectionProfile {
        Objects.requireNonNull(ocppVersion, "NetworkConnectionProfile.ocppVersion must not be null");
        Objects.requireNonNull(ocppTransport, "NetworkConnectionProfile.ocppTransport must not be null");
        Objects.requireNonNull(ocppInterface, "NetworkConnectionProfile.ocppInterface must not be null");
        if (messageTimeoutSeconds <= 0) {
            throw new IllegalArgumentException(
                    "NetworkConnectionProfile.messageTimeoutSeconds must be > 0, got " + messageTimeoutSeconds);
        }
        if (securityProfile < 1 || securityProfile > 3) {
            throw new IllegalArgumentException(
                    "NetworkConnectionProfile.securityProfile must be in [1, 3], got " + securityProfile);
        }
        Objects.requireNonNull(ocppCsmsUrl, "NetworkConnectionProfile.ocppCsmsUrl must not be null");
        if (ocppCsmsUrl.isBlank()) {
            throw new IllegalArgumentException("NetworkConnectionProfile.ocppCsmsUrl must not be blank");
        }
        if (ocppCsmsUrl.length() > URL_MAX) {
            throw new IllegalArgumentException(
                    "NetworkConnectionProfile.ocppCsmsUrl must be <= " + URL_MAX
                            + " chars, got " + ocppCsmsUrl.length());
        }
    }

    /**
     * Convenience factory for a plain WebSocket profile (no APN, no VPN) — the
     * common shape when the station connects over a fixed wired interface.
     */
    public static NetworkConnectionProfile ofWebSocket(OcppVersion ocppVersion,
                                                        OcppInterface ocppInterface,
                                                        String ocppCsmsUrl,
                                                        int messageTimeoutSeconds,
                                                        int securityProfile) {
        return new NetworkConnectionProfile(ocppVersion, OcppTransport.JSON, ocppInterface,
                messageTimeoutSeconds, securityProfile, ocppCsmsUrl, null, null);
    }
}
