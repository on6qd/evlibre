package com.evlibre.server.core.domain.v201.network;

/**
 * OCPP 2.0.1 {@code SetNetworkProfileStatusEnumType}: outcome of a B09
 * {@code SetNetworkProfile} request.
 *
 * <p>Wire form uses PascalCase ({@code Accepted}, {@code Rejected},
 * {@code Failed}). Per spec §1.35, {@code Accepted} means the station has
 * stored the profile — it only takes effect after the next reboot, which the
 * CSMS must schedule separately via {@code Reset}.
 */
public enum SetNetworkProfileStatus {
    /** Profile validated and stored; activation requires a reboot. */
    ACCEPTED,
    /** Profile was invalid or the station refused to store it. */
    REJECTED,
    /** The station attempted to store the profile but hit an internal error. */
    FAILED
}
