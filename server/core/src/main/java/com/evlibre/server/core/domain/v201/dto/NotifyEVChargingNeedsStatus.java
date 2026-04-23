package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code NotifyEVChargingNeedsStatusEnumType} — CSMS's answer when
 * the station forwards an EV-calculated charging needs request.
 *
 * <ul>
 *   <li>{@link #ACCEPTED}   — CSMS will provide a schedule momentarily.
 *   <li>{@link #REJECTED}   — CSMS will not provide a schedule.
 *   <li>{@link #PROCESSING} — CSMS is gathering info; caller should expect a schedule later.
 * </ul>
 */
public enum NotifyEVChargingNeedsStatus {
    ACCEPTED,
    REJECTED,
    PROCESSING
}
