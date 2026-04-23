package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.NotifyEVChargingNeedsStatus;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingNeeds;

/**
 * Inbound port for {@code NotifyEVChargingNeedsRequest} (ISO 15118 flow).
 * Unlike the other pass-through inbound ports in v201, this one returns a
 * {@link NotifyEVChargingNeedsStatus} because the response payload carries the
 * CSMS's decision (Accepted / Rejected / Processing) — the station will act on
 * it (provide/withhold a schedule via SetChargingProfile).
 */
public interface HandleNotifyEVChargingNeedsPort {

    NotifyEVChargingNeedsStatus handleNotifyEVChargingNeeds(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int evseId,
            Integer maxScheduleTuples,
            ChargingNeeds chargingNeeds);
}
