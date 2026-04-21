package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.common.model.ConnectorId;
import com.evlibre.common.model.EvseId;
import com.evlibre.common.model.MeterValue;
import com.evlibre.common.model.SampledValue;
import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.dto.TransactionEventData;
import com.evlibre.server.core.domain.ports.inbound.HandleTransactionEventPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TransactionEventHandler201 implements OcppMessageHandler {

    private final HandleTransactionEventPort transactionEventPort;
    private final ObjectMapper objectMapper;

    public TransactionEventHandler201(HandleTransactionEventPort transactionEventPort, ObjectMapper objectMapper) {
        this.transactionEventPort = transactionEventPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        String eventType = payload.path("eventType").asText();
        String transactionId = payload.path("transactionInfo").path("transactionId").asText();
        Instant timestamp = Instant.parse(payload.path("timestamp").asText());
        String triggerReason = payload.path("triggerReason").asText();

        // EVSE is optional
        EvseId evseId = null;
        ConnectorId connectorId = null;
        JsonNode evseNode = payload.path("evse");
        if (!evseNode.isMissingNode()) {
            evseId = new EvseId(evseNode.path("id").asInt());
            if (evseNode.has("connectorId")) {
                connectorId = new ConnectorId(evseNode.path("connectorId").asInt());
            }
        }

        // idToken is optional
        String idToken = null;
        JsonNode idTokenNode = payload.path("idToken");
        if (!idTokenNode.isMissingNode()) {
            idToken = idTokenNode.path("idToken").asText(null);
        }

        // meterValue is optional
        List<MeterValue> meterValues = parseMeterValues(payload.path("meterValue"));

        TransactionEventData data = new TransactionEventData(
                session.tenantId(), session.stationIdentity(),
                eventType, transactionId, idToken, triggerReason,
                evseId, connectorId, timestamp, meterValues
        );

        transactionEventPort.transactionEvent(data);

        return objectMapper.createObjectNode();
    }

    private List<MeterValue> parseMeterValues(JsonNode meterValueArray) {
        if (!meterValueArray.isArray()) {
            return null;
        }
        List<MeterValue> meterValues = new ArrayList<>();
        for (JsonNode mv : meterValueArray) {
            Instant timestamp = Instant.parse(mv.path("timestamp").asText());
            List<SampledValue> sampledValues = new ArrayList<>();
            JsonNode svArray = mv.path("sampledValue");
            if (svArray.isArray()) {
                for (JsonNode sv : svArray) {
                    sampledValues.add(new SampledValue(
                            sv.path("value").asText(),
                            sv.path("context").asText(null),
                            null, // format not in 2.0.1
                            sv.path("measurand").asText(null),
                            sv.path("phase").asText(null),
                            sv.path("location").asText(null),
                            sv.has("unitOfMeasure") ? sv.path("unitOfMeasure").path("unit").asText(null) : null
                    ));
                }
            }
            meterValues.add(new MeterValue(timestamp, sampledValues));
        }
        return meterValues;
    }
}
