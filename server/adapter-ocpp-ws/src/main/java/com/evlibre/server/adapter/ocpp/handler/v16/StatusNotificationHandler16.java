package com.evlibre.server.adapter.ocpp.handler.v16;

import com.evlibre.common.model.ConnectorId;
import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.dto.StatusNotificationData;
import com.evlibre.server.core.domain.model.ConnectorStatus;
import com.evlibre.server.core.domain.ports.inbound.HandleStatusNotificationPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

public class StatusNotificationHandler16 implements OcppMessageHandler {

    private final HandleStatusNotificationPort statusPort;
    private final ObjectMapper objectMapper;

    public StatusNotificationHandler16(HandleStatusNotificationPort statusPort, ObjectMapper objectMapper) {
        this.statusPort = statusPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        ConnectorId connectorId = new ConnectorId(payload.path("connectorId").asInt());
        ConnectorStatus status = mapStatus(payload.path("status").asText());
        String errorCode = payload.path("errorCode").asText("NoError");
        String timestampStr = payload.path("timestamp").asText(null);
        Instant timestamp = timestampStr != null ? Instant.parse(timestampStr) : Instant.now();
        // OCPP 1.6 §4.9: optional diagnostic fields — preserve them so operators can see
        // vendor-specific error detail alongside the generic errorCode.
        String info = payload.hasNonNull("info") ? payload.get("info").asText() : null;
        String vendorId = payload.hasNonNull("vendorId") ? payload.get("vendorId").asText() : null;
        String vendorErrorCode = payload.hasNonNull("vendorErrorCode") ? payload.get("vendorErrorCode").asText() : null;

        StatusNotificationData data = new StatusNotificationData(
                session.tenantId(), session.stationIdentity(),
                connectorId, status, errorCode, timestamp,
                info, vendorId, vendorErrorCode
        );

        statusPort.statusNotification(data);

        return objectMapper.createObjectNode();
    }

    private ConnectorStatus mapStatus(String ocppStatus) {
        return switch (ocppStatus) {
            case "Available" -> ConnectorStatus.AVAILABLE;
            case "Preparing" -> ConnectorStatus.PREPARING;
            case "Charging" -> ConnectorStatus.CHARGING;
            case "SuspendedEV" -> ConnectorStatus.SUSPENDED_EV;
            case "SuspendedEVSE" -> ConnectorStatus.SUSPENDED_EVSE;
            case "Finishing" -> ConnectorStatus.FINISHING;
            case "Reserved" -> ConnectorStatus.RESERVED;
            case "Unavailable" -> ConnectorStatus.UNAVAILABLE;
            case "Faulted" -> ConnectorStatus.FAULTED;
            default -> ConnectorStatus.UNAVAILABLE;
        };
    }
}
