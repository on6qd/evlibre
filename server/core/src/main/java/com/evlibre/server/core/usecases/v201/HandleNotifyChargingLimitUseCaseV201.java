package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyChargingLimitPort;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimit;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Pass-through handler for {@code NotifyChargingLimitRequest}. Forwards each
 * notification to a {@link Sink} so downstream subscribers (UI, audit log,
 * operator dashboards) can react without the use case taking a dependency on a
 * particular storage layer.
 */
public class HandleNotifyChargingLimitUseCaseV201 implements HandleNotifyChargingLimitPort {

    @FunctionalInterface
    public interface Sink {
        void onNotify(TenantId tenantId,
                      ChargePointIdentity stationIdentity,
                      Integer evseId,
                      ChargingLimit chargingLimit,
                      List<ChargingSchedule> schedules);
    }

    private static final Logger log = LoggerFactory.getLogger(HandleNotifyChargingLimitUseCaseV201.class);

    private final Sink sink;

    public HandleNotifyChargingLimitUseCaseV201(Sink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    @Override
    public void handleNotifyChargingLimit(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            Integer evseId,
            ChargingLimit chargingLimit,
            List<ChargingSchedule> schedules) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(chargingLimit, "chargingLimit");
        log.info("NotifyChargingLimit from {} (evseId={}, source={}, gridCritical={}, schedules={})",
                stationIdentity.value(), evseId,
                chargingLimit.chargingLimitSource(), chargingLimit.isGridCritical(),
                schedules == null ? 0 : schedules.size());
        sink.onNotify(tenantId, stationIdentity, evseId, chargingLimit, schedules);
    }
}
