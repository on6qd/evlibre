package com.evlibre.server.core.usecases.v201;

import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import com.evlibre.server.core.domain.v201.dto.StatusNotificationData201;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleStatusNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleStatusNotificationUseCaseV201 implements HandleStatusNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(HandleStatusNotificationUseCaseV201.class);

    private final OcppEventLogPort eventLog;

    public HandleStatusNotificationUseCaseV201(OcppEventLogPort eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public void statusNotification(StatusNotificationData201 data) {
        String detail = "evse=" + data.evseId().value()
                + " connector=" + data.connectorId().value()
                + " status=" + data.status();

        log.info("StatusNotification from {}: {}", data.stationIdentity().value(), detail);

        eventLog.logEvent(
                data.stationIdentity().value(),
                null,
                "StatusNotification",
                "IN",
                detail
        );
    }
}
