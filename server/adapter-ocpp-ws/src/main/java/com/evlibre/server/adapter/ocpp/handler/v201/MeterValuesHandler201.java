package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.common.model.EvseId;
import com.evlibre.common.model.MeterValue;
import com.evlibre.common.model.SampledValue;
import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.dto.MeterValuesData201;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleMeterValuesPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MeterValuesHandler201 implements OcppMessageHandler {

    private final HandleMeterValuesPort meterValuesPort;
    private final ObjectMapper objectMapper;

    public MeterValuesHandler201(HandleMeterValuesPort meterValuesPort, ObjectMapper objectMapper) {
        this.meterValuesPort = meterValuesPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        int evseId = payload.path("evseId").asInt();

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
                                null, // format field is 1.6-only
                                sv.path("measurand").asText(null),
                                sv.path("phase").asText(null),
                                sv.path("location").asText(null),
                                sv.has("unitOfMeasure") ? sv.path("unitOfMeasure").path("unit").asText(null) : null
                        ));
                    }
                }
                meterValues.add(new MeterValue(timestamp, sampledValues));
            }
        }

        MeterValuesData201 data = new MeterValuesData201(
                session.tenantId(), session.stationIdentity(),
                new EvseId(evseId), meterValues
        );
        meterValuesPort.meterValues(data);

        return objectMapper.createObjectNode();
    }
}
