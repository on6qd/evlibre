package com.evlibre.server.core.usecases;

import com.evlibre.server.core.domain.dto.StatusNotificationData;
import com.evlibre.server.core.domain.ports.inbound.HandleStatusNotificationPort;
import com.evlibre.server.core.domain.ports.outbound.OcppEventLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleStatusNotificationUseCase implements HandleStatusNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(HandleStatusNotificationUseCase.class);

    private final OcppEventLogPort eventLog;

    public HandleStatusNotificationUseCase(OcppEventLogPort eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public void statusNotification(StatusNotificationData data) {
        StringBuilder detail = new StringBuilder()
                .append("connector=").append(data.connectorId().value())
                .append(" status=").append(data.status())
                .append(" error=").append(data.errorCode());
        if (data.info() != null) detail.append(" info=").append(data.info());
        if (data.vendorId() != null) detail.append(" vendorId=").append(data.vendorId());
        if (data.vendorErrorCode() != null) detail.append(" vendorErrorCode=").append(data.vendorErrorCode());

        log.info("StatusNotification from {}: {}", data.stationIdentity().value(), detail);

        eventLog.logEvent(
                data.stationIdentity().value(),
                null,
                "StatusNotification",
                "IN",
                detail.toString()
        );
    }
}
