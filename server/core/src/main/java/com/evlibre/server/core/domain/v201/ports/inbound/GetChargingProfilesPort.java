package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GetChargingProfilesResult;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfileCriterion;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code GetChargingProfiles} for OCPP 2.0.1 (use case K09,
 * 2.0.1-only). The immediate response only tells the CSMS whether any profile
 * matched; the profiles themselves follow in one or more inbound
 * {@code ReportChargingProfilesRequest} messages sharing the same
 * {@code requestId}. This port returns only the immediate status — the CSMS
 * correlates the reports separately once the inbound handler lands.
 *
 * <p>K09.FR.04–06 evseId targeting:
 * <ul>
 *   <li>{@code evseId > 0}  — report profiles scoped to that EVSE.
 *   <li>{@code evseId = 0}  — report grid-connection-wide profiles.
 *   <li>{@code evseId = null} — report every matching profile on the station.
 * </ul>
 */
public interface GetChargingProfilesPort {

    CompletableFuture<GetChargingProfilesResult> getChargingProfiles(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int requestId,
            Integer evseId,
            ChargingProfileCriterion criterion);
}
