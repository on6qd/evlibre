package com.evlibre.server.adapter.ocpp;

import com.fasterxml.jackson.databind.JsonNode;

public record OcppCallResultMessage(String messageId, JsonNode payload) {}
