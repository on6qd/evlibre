package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.devicemodel.wire.DeviceModelWire;
import com.evlibre.server.core.domain.v201.diagnostics.DiagnosticsWire;
import com.evlibre.server.core.domain.v201.diagnostics.EventData;
import com.evlibre.server.core.domain.v201.diagnostics.EventNotificationType;
import com.evlibre.server.core.domain.v201.diagnostics.EventTrigger;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyEventPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotifyEventHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyEventHandler201.class);

    private final HandleNotifyEventPort port;
    private final ObjectMapper objectMapper;

    public NotifyEventHandler201(HandleNotifyEventPort port, ObjectMapper objectMapper) {
        this.port = port;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        Instant generatedAt = Instant.parse(payload.path("generatedAt").asText());
        int seqNo = payload.path("seqNo").asInt();
        boolean tbc = payload.hasNonNull("tbc") && payload.path("tbc").asBoolean();

        List<EventData> events = new ArrayList<>();
        JsonNode arr = payload.path("eventData");
        if (arr.isArray()) {
            for (JsonNode e : arr) {
                events.add(parseEventData(e));
            }
        }

        port.handleNotifyEvent(
                session.tenantId(), session.stationIdentity(),
                generatedAt, seqNo, tbc, events);

        log.info("NotifyEvent from {} (seqNo={}, tbc={}, events={})",
                session.stationIdentity().value(), seqNo, tbc, events.size());

        return objectMapper.createObjectNode();
    }

    @SuppressWarnings("unchecked")
    private EventData parseEventData(JsonNode node) {
        int eventId = node.path("eventId").asInt();
        Instant timestamp = Instant.parse(node.path("timestamp").asText());
        EventTrigger trigger = DiagnosticsWire.eventTriggerFromWire(node.path("trigger").asText());
        Integer cause = node.hasNonNull("cause") ? node.path("cause").asInt() : null;
        String actualValue = node.path("actualValue").asText();
        String techCode = node.hasNonNull("techCode") ? node.path("techCode").asText() : null;
        String techInfo = node.hasNonNull("techInfo") ? node.path("techInfo").asText() : null;
        Boolean cleared = node.hasNonNull("cleared") ? node.path("cleared").asBoolean() : null;
        String transactionId = node.hasNonNull("transactionId") ? node.path("transactionId").asText() : null;
        Integer variableMonitoringId = node.hasNonNull("variableMonitoringId")
                ? node.path("variableMonitoringId").asInt() : null;
        EventNotificationType notificationType = DiagnosticsWire.eventNotificationTypeFromWire(
                node.path("eventNotificationType").asText());
        Map<String, Object> componentMap = objectMapper.convertValue(node.path("component"), Map.class);
        Map<String, Object> variableMap = objectMapper.convertValue(node.path("variable"), Map.class);

        return new EventData(
                eventId, timestamp, trigger, cause, actualValue,
                techCode, techInfo, cleared, transactionId, variableMonitoringId,
                notificationType,
                DeviceModelWire.componentFromWire(componentMap),
                DeviceModelWire.variableFromWire(variableMap));
    }
}
