package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.diagnostics.UploadLogStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleLogStatusNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class HandleLogStatusNotificationUseCaseV201 implements HandleLogStatusNotificationPort {

    @FunctionalInterface
    public interface Sink {
        void onLogStatus(TenantId tenantId,
                         ChargePointIdentity stationIdentity,
                         UploadLogStatus status,
                         Integer requestId);
    }

    private static final Logger log = LoggerFactory.getLogger(HandleLogStatusNotificationUseCaseV201.class);

    private final Sink sink;

    public HandleLogStatusNotificationUseCaseV201(Sink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    @Override
    public void handleLogStatusNotification(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            UploadLogStatus status,
            Integer requestId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(status, "status");
        log.info("LogStatusNotification from {} (status={}, requestId={})",
                stationIdentity.value(), status, requestId);
        sink.onLogStatus(tenantId, stationIdentity, status, requestId);
    }
}
