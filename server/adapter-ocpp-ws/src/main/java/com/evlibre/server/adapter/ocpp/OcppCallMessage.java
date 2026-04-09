package com.evlibre.server.adapter.ocpp;

import com.fasterxml.jackson.databind.JsonNode;

public record OcppCallMessage(String messageId, String action, JsonNode payload) {}
