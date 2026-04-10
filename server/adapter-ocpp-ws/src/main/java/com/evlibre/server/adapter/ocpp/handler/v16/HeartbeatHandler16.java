package com.evlibre.server.adapter.ocpp.handler.v16;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.ports.inbound.HandleHeartbeatPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class HeartbeatHandler16 implements OcppMessageHandler {

    private final HandleHeartbeatPort heartbeatPort;
    private final ObjectMapper objectMapper;

    public HeartbeatHandler16(HandleHeartbeatPort heartbeatPort, ObjectMapper objectMapper) {
        this.heartbeatPort = heartbeatPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        Instant now = heartbeatPort.heartbeat(session.tenantId(), session.stationIdentity());

        ObjectNode response = objectMapper.createObjectNode();
        response.put("currentTime", DateTimeFormatter.ISO_INSTANT.format(now.atOffset(ZoneOffset.UTC)));
        return response;
    }
}
