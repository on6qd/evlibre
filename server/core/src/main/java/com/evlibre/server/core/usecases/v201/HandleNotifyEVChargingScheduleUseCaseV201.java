package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GenericStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyEVChargingSchedulePort;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;

public class HandleNotifyEVChargingScheduleUseCaseV201 implements HandleNotifyEVChargingSchedulePort {

    @FunctionalInterface
    public interface Policy {
        GenericStatus decide(
                TenantId tenantId,
                ChargePointIdentity stationIdentity,
                Instant timeBase,
                int evseId,
                ChargingSchedule chargingSchedule);
    }

    private static final Logger log = LoggerFactory.getLogger(HandleNotifyEVChargingScheduleUseCaseV201.class);

    private final Policy policy;

    public HandleNotifyEVChargingScheduleUseCaseV201(Policy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    public GenericStatus handleNotifyEVChargingSchedule(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            Instant timeBase,
            int evseId,
            ChargingSchedule chargingSchedule) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(timeBase, "timeBase");
        Objects.requireNonNull(chargingSchedule, "chargingSchedule");
        log.info("NotifyEVChargingSchedule from {} (evseId={}, timeBase={}, periods={})",
                stationIdentity.value(), evseId, timeBase,
                chargingSchedule.chargingSchedulePeriod().size());
        return policy.decide(tenantId, stationIdentity, timeBase, evseId, chargingSchedule);
    }
}
