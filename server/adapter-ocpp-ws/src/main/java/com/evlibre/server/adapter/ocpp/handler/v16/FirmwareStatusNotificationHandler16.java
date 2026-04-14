package com.evlibre.server.adapter.ocpp.handler.v16;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.ports.inbound.HandleFirmwareStatusPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FirmwareStatusNotificationHandler16 implements OcppMessageHandler {

    private final HandleFirmwareStatusPort firmwareStatusPort;
    private final ObjectMapper objectMapper;

    public FirmwareStatusNotificationHandler16(HandleFirmwareStatusPort firmwareStatusPort,
                                                ObjectMapper objectMapper) {
        this.firmwareStatusPort = firmwareStatusPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        String status = payload.path("status").asText();
        firmwareStatusPort.handleFirmwareStatus(session.tenantId(), session.stationIdentity(), status);
        return objectMapper.createObjectNode();
    }
}
