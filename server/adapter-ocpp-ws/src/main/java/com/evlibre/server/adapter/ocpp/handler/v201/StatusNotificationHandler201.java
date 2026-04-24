package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.common.model.ConnectorId;
import com.evlibre.common.model.EvseId;
import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.shared.model.ConnectorStatus;
import com.evlibre.server.core.domain.v201.dto.StatusNotificationData201;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleStatusNotificationPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

public class StatusNotificationHandler201 implements OcppMessageHandler {

    private final HandleStatusNotificationPort statusPort;
    private final ObjectMapper objectMapper;

    public StatusNotificationHandler201(HandleStatusNotificationPort statusPort, ObjectMapper objectMapper) {
        this.statusPort = statusPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        Instant timestamp = Instant.parse(payload.path("timestamp").asText());
        ConnectorStatus status = mapStatus(payload.path("connectorStatus").asText());
        int evseId = payload.path("evseId").asInt();
        int connectorId = payload.path("connectorId").asInt();

        StatusNotificationData201 data = new StatusNotificationData201(
                session.tenantId(), session.stationIdentity(),
                new EvseId(evseId), new ConnectorId(connectorId),
                status, timestamp
        );

        statusPort.statusNotification(data);

        return objectMapper.createObjectNode();
    }

    private ConnectorStatus mapStatus(String ocppStatus) {
        return switch (ocppStatus) {
            case "Available" -> ConnectorStatus.AVAILABLE;
            case "Occupied" -> ConnectorStatus.CHARGING;
            case "Reserved" -> ConnectorStatus.RESERVED;
            case "Unavailable" -> ConnectorStatus.UNAVAILABLE;
            case "Faulted" -> ConnectorStatus.FAULTED;
            default -> ConnectorStatus.UNAVAILABLE;
        };
    }
}
