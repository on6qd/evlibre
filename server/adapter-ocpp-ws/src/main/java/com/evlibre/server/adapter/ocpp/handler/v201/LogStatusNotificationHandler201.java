package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.diagnostics.DiagnosticsWire;
import com.evlibre.server.core.domain.v201.diagnostics.UploadLogStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleLogStatusNotificationPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogStatusNotificationHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(LogStatusNotificationHandler201.class);

    private final HandleLogStatusNotificationPort port;
    private final ObjectMapper objectMapper;

    public LogStatusNotificationHandler201(HandleLogStatusNotificationPort port, ObjectMapper objectMapper) {
        this.port = port;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        UploadLogStatus status = DiagnosticsWire.uploadLogStatusFromWire(payload.path("status").asText());
        Integer requestId = payload.hasNonNull("requestId") ? payload.path("requestId").asInt() : null;

        port.handleLogStatusNotification(
                session.tenantId(), session.stationIdentity(), status, requestId);

        log.info("LogStatusNotification from {} (status={}, requestId={})",
                session.stationIdentity().value(), status, requestId);

        return objectMapper.createObjectNode();
    }
}
