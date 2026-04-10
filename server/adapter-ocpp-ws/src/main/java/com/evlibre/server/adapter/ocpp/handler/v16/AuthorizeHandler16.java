package com.evlibre.server.adapter.ocpp.handler.v16;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.dto.AuthorizationResult;
import com.evlibre.server.core.domain.ports.inbound.AuthorizePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AuthorizeHandler16 implements OcppMessageHandler {

    private final AuthorizePort authorizePort;
    private final ObjectMapper objectMapper;

    public AuthorizeHandler16(AuthorizePort authorizePort, ObjectMapper objectMapper) {
        this.authorizePort = authorizePort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        String idTag = payload.path("idTag").asText();

        AuthorizationResult result = authorizePort.authorize(session.tenantId(), idTag);

        ObjectNode idTagInfo = objectMapper.createObjectNode();
        idTagInfo.put("status", mapStatus(result.status().name()));

        ObjectNode response = objectMapper.createObjectNode();
        response.set("idTagInfo", idTagInfo);

        return response;
    }

    private String mapStatus(String status) {
        return switch (status) {
            case "ACCEPTED" -> "Accepted";
            case "BLOCKED" -> "Blocked";
            case "EXPIRED" -> "Expired";
            case "INVALID" -> "Invalid";
            case "CONCURRENT_TX" -> "ConcurrentTx";
            default -> "Invalid";
        };
    }
}
