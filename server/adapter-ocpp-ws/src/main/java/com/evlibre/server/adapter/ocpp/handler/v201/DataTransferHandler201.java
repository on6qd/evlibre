package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.dto.DataTransferResult;
import com.evlibre.server.core.domain.v201.dto.DataTransferStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleDataTransferPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DataTransferHandler201 implements OcppMessageHandler {

    private final HandleDataTransferPort handleDataTransferPort;
    private final ObjectMapper objectMapper;

    public DataTransferHandler201(HandleDataTransferPort handleDataTransferPort, ObjectMapper objectMapper) {
        this.handleDataTransferPort = handleDataTransferPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        String vendorId = payload.path("vendorId").asText();
        String msgId = payload.has("messageId") ? payload.get("messageId").asText() : null;
        // `data` is anyType per the 2.0.1 spec — can be a primitive, array, or object.
        // Convert to a plain Java tree (Map/List/String/Number/Boolean) so the domain
        // port stays free of Jackson types.
        Object data = payload.has("data")
                ? objectMapper.convertValue(payload.get("data"), Object.class)
                : null;

        DataTransferResult result = handleDataTransferPort.handleDataTransfer(
                session.tenantId(), vendorId, msgId, data);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", toWire(result.status()));
        return response;
    }

    private static String toWire(DataTransferStatus status) {
        return switch (status) {
            case ACCEPTED -> "Accepted";
            case REJECTED -> "Rejected";
            case UNKNOWN_MESSAGE_ID -> "UnknownMessageId";
            case UNKNOWN_VENDOR_ID -> "UnknownVendorId";
        };
    }
}
