package com.evlibre.server.core.domain.v201.smartcharging;

/**
 * OCPP 2.0.1 {@code ChargingProfilePurposeEnumType} — what a charging profile is
 * for. Interacts with {@code evseId} in the set request (K01.FR):
 *
 * <ul>
 *   <li>{@link #CHARGING_STATION_MAX_PROFILE}          — station-wide ceiling; must use {@code evseId = 0}.
 *   <li>{@link #CHARGING_STATION_EXTERNAL_CONSTRAINTS} — grid/ext constraints; must use {@code evseId = 0}.
 *   <li>{@link #TX_DEFAULT_PROFILE}                    — default for transactions; {@code evseId = 0} applies to every EVSE, otherwise scoped.
 *   <li>{@link #TX_PROFILE}                            — tied to a specific transaction; requires {@code evseId > 0} and {@code transactionId}.
 * </ul>
 */
public enum ChargingProfilePurpose {
    CHARGING_STATION_EXTERNAL_CONSTRAINTS,
    CHARGING_STATION_MAX_PROFILE,
    TX_DEFAULT_PROFILE,
    TX_PROFILE
}
