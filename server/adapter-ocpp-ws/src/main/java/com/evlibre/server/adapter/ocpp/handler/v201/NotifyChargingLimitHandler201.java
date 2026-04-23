package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyChargingLimitPort;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimit;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedule;
import com.evlibre.server.core.domain.v201.smartcharging.wire.ChargingProfileWire;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotifyChargingLimitHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyChargingLimitHandler201.class);

    private final HandleNotifyChargingLimitPort port;
    private final ObjectMapper objectMapper;

    public NotifyChargingLimitHandler201(HandleNotifyChargingLimitPort port, ObjectMapper objectMapper) {
        this.port = port;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        Integer evseId = payload.hasNonNull("evseId") ? payload.path("evseId").asInt() : null;

        JsonNode limitNode = payload.path("chargingLimit");
        ChargingLimitSource source = ChargingProfileWire.limitSourceFromWire(
                limitNode.path("chargingLimitSource").asText());
        Boolean gridCritical = limitNode.hasNonNull("isGridCritical")
                ? limitNode.path("isGridCritical").asBoolean() : null;
        ChargingLimit limit = new ChargingLimit(source, gridCritical);

        List<ChargingSchedule> schedules = new ArrayList<>();
        JsonNode schedulesNode = payload.path("chargingSchedule");
        if (schedulesNode.isArray()) {
            for (JsonNode s : schedulesNode) {
                @SuppressWarnings("unchecked")
                Map<String, Object> asMap = objectMapper.convertValue(s, Map.class);
                schedules.add(ChargingProfileWire.chargingScheduleFromWire(asMap));
            }
        }

        port.handleNotifyChargingLimit(
                session.tenantId(), session.stationIdentity(),
                evseId, limit, schedules);

        log.info("NotifyChargingLimit from {} (evseId={}, source={}, gridCritical={}, schedules={})",
                session.stationIdentity().value(), evseId, source, gridCritical, schedules.size());

        return objectMapper.createObjectNode();
    }
}
