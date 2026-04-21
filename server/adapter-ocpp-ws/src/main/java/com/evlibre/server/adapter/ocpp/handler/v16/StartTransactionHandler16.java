package com.evlibre.server.adapter.ocpp.handler.v16;

import com.evlibre.common.model.ConnectorId;
import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v16.dto.StartTransactionData;
import com.evlibre.server.core.domain.v16.dto.StartTransactionResult;
import com.evlibre.server.core.domain.ports.inbound.StartTransactionPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;

public class StartTransactionHandler16 implements OcppMessageHandler {

    private final StartTransactionPort startTransactionPort;
    private final ObjectMapper objectMapper;

    public StartTransactionHandler16(StartTransactionPort startTransactionPort, ObjectMapper objectMapper) {
        this.startTransactionPort = startTransactionPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        ConnectorId connectorId = new ConnectorId(payload.path("connectorId").asInt());
        String idTag = payload.path("idTag").asText();
        long meterStart = payload.path("meterStart").asLong();
        String timestampStr = payload.path("timestamp").asText(null);
        Instant timestamp = timestampStr != null ? Instant.parse(timestampStr) : Instant.now();
        Integer reservationId = payload.hasNonNull("reservationId") ? payload.get("reservationId").asInt() : null;

        StartTransactionData data = new StartTransactionData(
                session.tenantId(), session.stationIdentity(),
                connectorId, idTag, meterStart, timestamp, reservationId
        );

        StartTransactionResult result = startTransactionPort.startTransaction(data);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("transactionId", result.transactionId());
        response.set("idTagInfo",
                AuthorizeHandler16.buildIdTagInfo(result.toAuthorizationResult(idTag), objectMapper));

        return response;
    }
}
