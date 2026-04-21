package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v16.dto.AuthorizationResult;
import com.evlibre.server.core.domain.v201.ports.inbound.AuthorizePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AuthorizeHandler201 implements OcppMessageHandler {

    private final AuthorizePort authorizePort;
    private final ObjectMapper objectMapper;

    public AuthorizeHandler201(AuthorizePort authorizePort, ObjectMapper objectMapper) {
        this.authorizePort = authorizePort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        String idToken = payload.path("idToken").path("idToken").asText();

        AuthorizationResult result = authorizePort.authorize(session.tenantId(), idToken);

        ObjectNode idTokenInfo = objectMapper.createObjectNode();
        idTokenInfo.put("status", mapStatus(result.status().name()));

        ObjectNode response = objectMapper.createObjectNode();
        response.set("idTokenInfo", idTokenInfo);

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
