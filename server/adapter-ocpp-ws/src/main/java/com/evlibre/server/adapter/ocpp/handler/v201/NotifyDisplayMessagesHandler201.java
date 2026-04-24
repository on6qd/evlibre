package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.displaymessage.MessageInfo;
import com.evlibre.server.core.domain.v201.displaymessage.wire.DisplayMessageWire;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyDisplayMessagesPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NotifyDisplayMessagesHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyDisplayMessagesHandler201.class);

    private final HandleNotifyDisplayMessagesPort handler;
    private final ObjectMapper objectMapper;

    public NotifyDisplayMessagesHandler201(HandleNotifyDisplayMessagesPort handler, ObjectMapper objectMapper) {
        this.handler = handler;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        int requestId = payload.path("requestId").asInt();
        boolean tbc = payload.path("tbc").asBoolean(false);
        JsonNode arr = payload.path("messageInfo");

        List<MessageInfo> messages = new ArrayList<>();
        if (arr.isArray()) {
            for (JsonNode entry : arr) {
                messages.add(DisplayMessageWire.messageInfoFromWire(toMap(entry)));
            }
        }

        handler.handleFrame(session.tenantId(), session.stationIdentity(),
                requestId, tbc, messages);

        log.info("NotifyDisplayMessages from {} (requestId={}, messages={}, tbc={})",
                session.stationIdentity().value(), requestId, messages.size(), tbc);

        return objectMapper.createObjectNode();
    }

    private Map<String, Object> toMap(JsonNode node) {
        return objectMapper.convertValue(node, new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>>() {});
    }
}
