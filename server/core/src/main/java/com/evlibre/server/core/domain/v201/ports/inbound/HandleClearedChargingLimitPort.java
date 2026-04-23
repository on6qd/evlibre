package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;

/**
 * Inbound port for {@code ClearedChargingLimitRequest} (K13.FR.02 / K14.FR.03).
 * Station calls this when an external limit is withdrawn. {@code evseId} is
 * optional — absent for station-wide clears.
 */
public interface HandleClearedChargingLimitPort {

    void handleClearedChargingLimit(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            ChargingLimitSource chargingLimitSource,
            Integer evseId);
}
