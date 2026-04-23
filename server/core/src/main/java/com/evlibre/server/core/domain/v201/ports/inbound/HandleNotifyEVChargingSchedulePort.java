package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GenericStatus;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedule;

import java.time.Instant;

/**
 * Inbound port for {@code NotifyEVChargingScheduleRequest} (ISO 15118 flow).
 * Station forwards an EV-calculated schedule; {@code timeBase} anchors every
 * period's relative start. Returns {@link GenericStatus#ACCEPTED} or
 * {@link GenericStatus#REJECTED} — per spec, acceptance only means the CSMS
 * successfully received the schedule, not that it approves it.
 */
public interface HandleNotifyEVChargingSchedulePort {

    GenericStatus handleNotifyEVChargingSchedule(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            Instant timeBase,
            int evseId,
            ChargingSchedule chargingSchedule);
}
