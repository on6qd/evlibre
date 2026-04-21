package com.evlibre.server.core.usecases.v201;

import com.evlibre.server.core.domain.v16.dto.StatusNotificationData;
import com.evlibre.server.core.domain.shared.model.ConnectorStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleStatusNotificationPort;
import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class HandleStatusNotificationUseCaseV201 implements HandleStatusNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(HandleStatusNotificationUseCaseV201.class);

    // OCPP 1.6 §4.9: only these statuses are valid for connectorId=0 (station-wide).
    private static final Set<ConnectorStatus> CONNECTOR_0_VALID_STATUSES = Set.of(
            ConnectorStatus.AVAILABLE, ConnectorStatus.UNAVAILABLE, ConnectorStatus.FAULTED);

    private final OcppEventLogPort eventLog;

    public HandleStatusNotificationUseCaseV201(OcppEventLogPort eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public void statusNotification(StatusNotificationData data) {
        if (data.connectorId().value() == 0 && !CONNECTOR_0_VALID_STATUSES.contains(data.status())) {
            log.warn("Non-compliant StatusNotification from {}: connectorId=0 only permits "
                            + "Available/Unavailable/Faulted, got {}",
                    data.stationIdentity().value(), data.status());
        }

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
