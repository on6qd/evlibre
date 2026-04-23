package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimit;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedule;

import java.util.List;

/**
 * Inbound port for {@code NotifyChargingLimitRequest} (K13/K14). Station calls
 * this when an external system (EMS, SO, CSO, …) imposes a charging limit it
 * can't refuse.
 *
 * <p>{@code evseId} is optional (null when the limit is station-wide). The
 * {@code schedules} list is also optional — stations omit it when
 * {@code NotifyChargingLimitWithSchedules} is configured false to save
 * bandwidth — so callers should not assume a schedule is always present.
 */
public interface HandleNotifyChargingLimitPort {

    void handleNotifyChargingLimit(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            Integer evseId,
            ChargingLimit chargingLimit,
            List<ChargingSchedule> schedules);
}
