package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.firmware.PublishFirmwareStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.HandlePublishFirmwareStatusNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class HandlePublishFirmwareStatusNotificationUseCaseV201
        implements HandlePublishFirmwareStatusNotificationPort {

    @FunctionalInterface
    public interface Sink {
        void onPublishStatus(TenantId tenantId,
                             ChargePointIdentity stationIdentity,
                             PublishFirmwareStatus status,
                             List<String> locations,
                             Integer requestId);
    }

    private static final Logger log = LoggerFactory.getLogger(
            HandlePublishFirmwareStatusNotificationUseCaseV201.class);

    private final Sink sink;

    public HandlePublishFirmwareStatusNotificationUseCaseV201(Sink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    @Override
    public void handlePublishFirmwareStatusNotification(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            PublishFirmwareStatus status,
            List<String> locations,
            Integer requestId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(locations, "locations");
        // L03.FR.04 — the Published status MUST come with at least one location.
        if (status == PublishFirmwareStatus.PUBLISHED && locations.isEmpty()) {
            throw new IllegalArgumentException(
                    "PublishFirmwareStatusNotification with status=Published requires at least one location");
        }
        log.info("PublishFirmwareStatusNotification from {} (status={}, requestId={}, locations={})",
                stationIdentity.value(), status, requestId, locations.size());
        sink.onPublishStatus(tenantId, stationIdentity, status, List.copyOf(locations), requestId);
    }
}
