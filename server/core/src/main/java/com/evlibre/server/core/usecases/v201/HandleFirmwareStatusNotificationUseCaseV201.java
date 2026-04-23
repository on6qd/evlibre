package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.firmware.FirmwareStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleFirmwareStatusNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class HandleFirmwareStatusNotificationUseCaseV201 implements HandleFirmwareStatusNotificationPort {

    @FunctionalInterface
    public interface Sink {
        void onFirmwareStatus(TenantId tenantId,
                              ChargePointIdentity stationIdentity,
                              FirmwareStatus status,
                              Integer requestId);
    }

    private static final Logger log = LoggerFactory.getLogger(HandleFirmwareStatusNotificationUseCaseV201.class);

    private final Sink sink;

    public HandleFirmwareStatusNotificationUseCaseV201(Sink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    @Override
    public void handleFirmwareStatusNotification(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            FirmwareStatus status,
            Integer requestId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(status, "status");
        log.info("FirmwareStatusNotification from {} (status={}, requestId={})",
                stationIdentity.value(), status, requestId);
        sink.onFirmwareStatus(tenantId, stationIdentity, status, requestId);
    }
}
