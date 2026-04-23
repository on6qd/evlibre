package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.firmware.FirmwareStatus;
import com.evlibre.server.core.domain.v201.firmware.FirmwareWire;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleFirmwareStatusNotificationPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirmwareStatusNotificationHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(FirmwareStatusNotificationHandler201.class);

    private final HandleFirmwareStatusNotificationPort port;
    private final ObjectMapper objectMapper;

    public FirmwareStatusNotificationHandler201(HandleFirmwareStatusNotificationPort port, ObjectMapper objectMapper) {
        this.port = port;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        FirmwareStatus status = FirmwareWire.statusFromWire(payload.path("status").asText());
        Integer requestId = payload.hasNonNull("requestId") ? payload.path("requestId").asInt() : null;

        port.handleFirmwareStatusNotification(
                session.tenantId(), session.stationIdentity(), status, requestId);

        log.info("FirmwareStatusNotification from {} (status={}, requestId={})",
                session.stationIdentity().value(), status, requestId);

        return objectMapper.createObjectNode();
    }
}
