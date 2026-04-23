package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.SetChargingProfileResult;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfile;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code SetChargingProfile} for OCPP 2.0.1 (use case K01).
 * Installs a charging profile onto the station, either station-wide
 * ({@code evseId = 0}) or scoped to a specific EVSE.
 *
 * <p>The {@code (evseId, profile.purpose)} pairing must satisfy K01's rules:
 * <ul>
 *   <li>{@code ChargingStationMaxProfile} and {@code ChargingStationExternalConstraints}
 *       require {@code evseId = 0};
 *   <li>{@code TxProfile} requires {@code evseId > 0} and
 *       {@code profile.transactionId} set;
 *   <li>{@code TxDefaultProfile} accepts either form.
 * </ul>
 * Invalid combinations surface as {@link IllegalArgumentException} before
 * anything is sent — the station-side checks remain authoritative for runtime
 * conditions like "transaction not found" or "duplicate profile".
 */
public interface SetChargingProfilePort {

    CompletableFuture<SetChargingProfileResult> setChargingProfile(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int evseId,
            ChargingProfile profile);
}
