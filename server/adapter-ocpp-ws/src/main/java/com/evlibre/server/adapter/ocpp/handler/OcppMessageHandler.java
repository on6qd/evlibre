package com.evlibre.server.adapter.ocpp.handler;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.fasterxml.jackson.databind.JsonNode;

public interface OcppMessageHandler {

    JsonNode handle(OcppSession session, String messageId, JsonNode payload);
}
