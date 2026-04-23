package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleClearedChargingLimitPort;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class HandleClearedChargingLimitUseCaseV201 implements HandleClearedChargingLimitPort {

    @FunctionalInterface
    public interface Sink {
        void onCleared(TenantId tenantId,
                       ChargePointIdentity stationIdentity,
                       ChargingLimitSource source,
                       Integer evseId);
    }

    private static final Logger log = LoggerFactory.getLogger(HandleClearedChargingLimitUseCaseV201.class);

    private final Sink sink;

    public HandleClearedChargingLimitUseCaseV201(Sink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    @Override
    public void handleClearedChargingLimit(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            ChargingLimitSource chargingLimitSource,
            Integer evseId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(chargingLimitSource, "chargingLimitSource");
        log.info("ClearedChargingLimit from {} (source={}, evseId={})",
                stationIdentity.value(), chargingLimitSource, evseId);
        sink.onCleared(tenantId, stationIdentity, chargingLimitSource, evseId);
    }
}
