package com.evlibre.server.adapter.ocpp.handler.v16;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.ports.inbound.HandleDataTransferPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DataTransferHandler16 implements OcppMessageHandler {

    private final HandleDataTransferPort handleDataTransferPort;
    private final ObjectMapper objectMapper;

    public DataTransferHandler16(HandleDataTransferPort handleDataTransferPort, ObjectMapper objectMapper) {
        this.handleDataTransferPort = handleDataTransferPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        String vendorId = payload.path("vendorId").asText();
        String msgId = payload.has("messageId") ? payload.get("messageId").asText() : null;
        String data = payload.has("data") ? payload.get("data").asText() : null;

        CommandResult result = handleDataTransferPort.handleDataTransfer(
                session.tenantId(), vendorId, msgId, data);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", result.status());
        return response;
    }
}
