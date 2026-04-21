package com.evlibre.server.adapter.ocpp.handler.v16;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v16.dto.StopTransactionData;
import com.evlibre.server.core.domain.v16.ports.inbound.StopTransactionPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;

public class StopTransactionHandler16 implements OcppMessageHandler {

    private final StopTransactionPort stopTransactionPort;
    private final ObjectMapper objectMapper;

    public StopTransactionHandler16(StopTransactionPort stopTransactionPort, ObjectMapper objectMapper) {
        this.stopTransactionPort = stopTransactionPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        int transactionId = payload.path("transactionId").asInt();
        String idTag = payload.path("idTag").asText(null);
        long meterStop = payload.path("meterStop").asLong();
        String timestampStr = payload.path("timestamp").asText(null);
        Instant timestamp = timestampStr != null ? Instant.parse(timestampStr) : Instant.now();
        String reason = payload.path("reason").asText(null);

        StopTransactionData data = new StopTransactionData(
                session.tenantId(), session.stationIdentity(),
                transactionId, idTag, meterStop, timestamp, reason
        );

        var authResult = stopTransactionPort.stopTransaction(data);

        // OCPP 1.6 §5.28: StopTransaction.conf carries idTagInfo iff an idTag was provided.
        ObjectNode response = objectMapper.createObjectNode();
        authResult.ifPresent(r -> response.set("idTagInfo",
                AuthorizeHandler16.buildIdTagInfo(r, objectMapper)));
        return response;
    }
}
