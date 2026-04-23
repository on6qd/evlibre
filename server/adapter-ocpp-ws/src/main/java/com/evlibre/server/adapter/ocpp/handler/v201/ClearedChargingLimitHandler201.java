package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleClearedChargingLimitPort;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;
import com.evlibre.server.core.domain.v201.smartcharging.wire.ChargingProfileWire;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClearedChargingLimitHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ClearedChargingLimitHandler201.class);

    private final HandleClearedChargingLimitPort port;
    private final ObjectMapper objectMapper;

    public ClearedChargingLimitHandler201(HandleClearedChargingLimitPort port, ObjectMapper objectMapper) {
        this.port = port;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        ChargingLimitSource source = ChargingProfileWire.limitSourceFromWire(
                payload.path("chargingLimitSource").asText());
        Integer evseId = payload.hasNonNull("evseId") ? payload.path("evseId").asInt() : null;

        port.handleClearedChargingLimit(
                session.tenantId(), session.stationIdentity(),
                source, evseId);

        log.info("ClearedChargingLimit from {} (source={}, evseId={})",
                session.stationIdentity().value(), source, evseId);

        return objectMapper.createObjectNode();
    }
}
