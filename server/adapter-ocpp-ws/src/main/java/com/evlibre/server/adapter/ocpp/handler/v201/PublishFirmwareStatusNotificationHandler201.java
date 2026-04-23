package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.firmware.FirmwareWire;
import com.evlibre.server.core.domain.v201.firmware.PublishFirmwareStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.HandlePublishFirmwareStatusNotificationPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PublishFirmwareStatusNotificationHandler201 implements OcppMessageHandler {

    private static final Logger log =
            LoggerFactory.getLogger(PublishFirmwareStatusNotificationHandler201.class);

    private final HandlePublishFirmwareStatusNotificationPort port;
    private final ObjectMapper objectMapper;

    public PublishFirmwareStatusNotificationHandler201(
            HandlePublishFirmwareStatusNotificationPort port, ObjectMapper objectMapper) {
        this.port = port;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        PublishFirmwareStatus status = FirmwareWire.publishStatusFromWire(
                payload.path("status").asText());
        Integer requestId = payload.hasNonNull("requestId") ? payload.path("requestId").asInt() : null;

        List<String> locations = new ArrayList<>();
        JsonNode locArr = payload.path("location");
        if (locArr.isArray()) {
            for (JsonNode l : locArr) {
                locations.add(l.asText());
            }
        }

        port.handlePublishFirmwareStatusNotification(
                session.tenantId(), session.stationIdentity(),
                status, locations, requestId);

        log.info("PublishFirmwareStatusNotification from {} (status={}, requestId={}, locations={})",
                session.stationIdentity().value(), status, requestId, locations.size());

        return objectMapper.createObjectNode();
    }
}
