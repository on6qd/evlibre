package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleSecurityEventNotificationPort;
import com.evlibre.server.core.domain.v201.security.SecurityEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class SecurityEventNotificationHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventNotificationHandler201.class);

    private final HandleSecurityEventNotificationPort port;
    private final ObjectMapper objectMapper;

    public SecurityEventNotificationHandler201(HandleSecurityEventNotificationPort port, ObjectMapper objectMapper) {
        this.port = port;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        String type = payload.path("type").asText();
        Instant timestamp = Instant.parse(payload.path("timestamp").asText());
        String techInfo = payload.hasNonNull("techInfo") ? payload.path("techInfo").asText() : null;

        SecurityEvent event = new SecurityEvent(type, timestamp, techInfo);

        port.handleSecurityEventNotification(
                session.tenantId(), session.stationIdentity(), event);

        log.info("SecurityEventNotification from {} (type={}, techInfo={})",
                session.stationIdentity().value(), type, techInfo);

        return objectMapper.createObjectNode();
    }
}
