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
        log.info("StatusNotification from {} connector {}: {} (error: {})",
                data.stationIdentity().value(), data.connectorId().value(),
                data.status(), data.errorCode());

        eventLog.logEvent(
                data.stationIdentity().value(),
                null,
                "StatusNotification",
                "IN",
                String.format("connector=%d status=%s error=%s",
                        data.connectorId().value(), data.status(), data.errorCode())
        );
    }
}
