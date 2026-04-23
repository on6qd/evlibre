package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.dto.NotifyEVChargingNeedsStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyEVChargingNeedsPort;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingNeeds;
import com.evlibre.server.core.domain.v201.smartcharging.wire.ChargingProfileWire;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class NotifyEVChargingNeedsHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyEVChargingNeedsHandler201.class);

    private final HandleNotifyEVChargingNeedsPort port;
    private final ObjectMapper objectMapper;

    public NotifyEVChargingNeedsHandler201(HandleNotifyEVChargingNeedsPort port, ObjectMapper objectMapper) {
        this.port = port;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        int evseId = payload.path("evseId").asInt();
        Integer maxScheduleTuples = payload.hasNonNull("maxScheduleTuples")
                ? payload.path("maxScheduleTuples").asInt() : null;

        @SuppressWarnings("unchecked")
        Map<String, Object> needsMap = objectMapper.convertValue(payload.path("chargingNeeds"), Map.class);
        ChargingNeeds needs = ChargingProfileWire.chargingNeedsFromWire(needsMap);

        NotifyEVChargingNeedsStatus status = port.handleNotifyEVChargingNeeds(
                session.tenantId(), session.stationIdentity(),
                evseId, maxScheduleTuples, needs);

        log.info("NotifyEVChargingNeeds from {} (evseId={}, mode={}): {}",
                session.stationIdentity().value(), evseId,
                needs.requestedEnergyTransfer(), status);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", statusToWire(status));
        return response;
    }

    private static String statusToWire(NotifyEVChargingNeedsStatus s) {
        return switch (s) {
            case ACCEPTED -> "Accepted";
            case REJECTED -> "Rejected";
            case PROCESSING -> "Processing";
        };
    }
}
