package com.evlibre.server.adapter.ocpp.handler.v16;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v16.dto.AuthorizationResult;
import com.evlibre.server.core.domain.ports.inbound.AuthorizePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.format.DateTimeFormatter;

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

        ObjectNode response = objectMapper.createObjectNode();
        response.set("idTagInfo", buildIdTagInfo(result, objectMapper));

        return response;
    }

    static ObjectNode buildIdTagInfo(AuthorizationResult result, ObjectMapper objectMapper) {
        ObjectNode idTagInfo = objectMapper.createObjectNode();
        idTagInfo.put("status", mapStatus(result.status().name()));
        if (result.expiryDate() != null) {
            idTagInfo.put("expiryDate", DateTimeFormatter.ISO_INSTANT.format(result.expiryDate()));
        }
        if (result.parentIdTag() != null) {
            idTagInfo.put("parentIdTag", result.parentIdTag());
        }
        return idTagInfo;
    }

    static String mapStatus(String status) {
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
