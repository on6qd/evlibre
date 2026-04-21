package com.evlibre.server.adapter.ocpp.handler.v16;

import com.evlibre.common.model.ConnectorId;
import com.evlibre.common.model.MeterValue;
import com.evlibre.common.model.SampledValue;
import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v16.dto.MeterValuesData;
import com.evlibre.server.core.domain.ports.inbound.HandleMeterValuesPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MeterValuesHandler16 implements OcppMessageHandler {

    private final HandleMeterValuesPort meterValuesPort;
    private final ObjectMapper objectMapper;

    public MeterValuesHandler16(HandleMeterValuesPort meterValuesPort, ObjectMapper objectMapper) {
        this.meterValuesPort = meterValuesPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        ConnectorId connectorId = new ConnectorId(payload.path("connectorId").asInt());
        Integer transactionId = payload.has("transactionId") ? payload.get("transactionId").asInt() : null;

        List<MeterValue> meterValues = new ArrayList<>();
        JsonNode meterValueArray = payload.path("meterValue");
        if (meterValueArray.isArray()) {
            for (JsonNode mv : meterValueArray) {
                Instant timestamp = Instant.parse(mv.path("timestamp").asText());
                List<SampledValue> sampledValues = new ArrayList<>();
                JsonNode svArray = mv.path("sampledValue");
                if (svArray.isArray()) {
                    for (JsonNode sv : svArray) {
                        sampledValues.add(new SampledValue(
                                sv.path("value").asText(),
                                sv.path("context").asText(null),
                                sv.path("format").asText(null),
                                sv.path("measurand").asText(null),
                                sv.path("phase").asText(null),
                                sv.path("location").asText(null),
                                sv.path("unit").asText(null)
                        ));
                    }
                }
                meterValues.add(new MeterValue(timestamp, sampledValues));
            }
        }

        MeterValuesData data = new MeterValuesData(
                session.tenantId(), session.stationIdentity(),
                connectorId, transactionId, meterValues
        );
        meterValuesPort.meterValues(data);

        return objectMapper.createObjectNode();
    }
}
