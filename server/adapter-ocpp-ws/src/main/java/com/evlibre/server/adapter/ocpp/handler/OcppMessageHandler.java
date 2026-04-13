package com.evlibre.server.adapter.ocpp.handler;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.fasterxml.jackson.databind.JsonNode;

public interface OcppMessageHandler {

    JsonNode handle(OcppSession session, String messageId, JsonNode payload);

    /**
     * Called after the response has been sent to the station.
     * Override to perform post-response actions like sending CSMS-initiated commands.
     */
    default void afterResponse(OcppSession session) {
        // no-op by default
    }
}
