package com.evlibre.server.adapter.ocpp.handler.v16;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v16.ports.inbound.HandleDiagnosticsStatusPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DiagnosticsStatusNotificationHandler16 implements OcppMessageHandler {

    private final HandleDiagnosticsStatusPort diagnosticsStatusPort;
    private final ObjectMapper objectMapper;

    public DiagnosticsStatusNotificationHandler16(HandleDiagnosticsStatusPort diagnosticsStatusPort,
                                                    ObjectMapper objectMapper) {
        this.diagnosticsStatusPort = diagnosticsStatusPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        String status = payload.path("status").asText();
        diagnosticsStatusPort.handleDiagnosticsStatus(session.tenantId(), session.stationIdentity(), status);
        return objectMapper.createObjectNode();
    }
}
