package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GetCompositeScheduleResult;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingRateUnit;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code GetCompositeSchedule} for OCPP 2.0.1 (use case K08).
 * Asks the station to collapse every currently-valid charging profile at an
 * EVSE (or the whole station when {@code evseId = 0}, K08.FR.03) into a single
 * composite schedule covering the next {@code duration} seconds.
 *
 * <p>The optional {@code chargingRateUnit} overrides which unit the station
 * renders the limits in; passing {@code null} lets the station choose. If
 * the station isn't configured for the requested unit it responds
 * {@code Rejected} (K08.FR.07).
 */
public interface GetCompositeSchedulePort {

    CompletableFuture<GetCompositeScheduleResult> getCompositeSchedule(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int evseId,
            int durationSeconds,
            ChargingRateUnit chargingRateUnit);
}
