package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.dto.GenericStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyEVChargingSchedulePort;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedule;
import com.evlibre.server.core.domain.v201.smartcharging.wire.ChargingProfileWire;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class NotifyEVChargingScheduleHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyEVChargingScheduleHandler201.class);

    private final HandleNotifyEVChargingSchedulePort port;
    private final ObjectMapper objectMapper;

    public NotifyEVChargingScheduleHandler201(HandleNotifyEVChargingSchedulePort port, ObjectMapper objectMapper) {
        this.port = port;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        Instant timeBase = Instant.parse(payload.path("timeBase").asText());
        int evseId = payload.path("evseId").asInt();

        @SuppressWarnings("unchecked")
        Map<String, Object> scheduleMap = objectMapper.convertValue(payload.path("chargingSchedule"), Map.class);
        ChargingSchedule schedule = ChargingProfileWire.chargingScheduleFromWire(scheduleMap);

        GenericStatus status = port.handleNotifyEVChargingSchedule(
                session.tenantId(), session.stationIdentity(),
                timeBase, evseId, schedule);

        log.info("NotifyEVChargingSchedule from {} (evseId={}, timeBase={}): {}",
                session.stationIdentity().value(), evseId, timeBase, status);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", status == GenericStatus.ACCEPTED ? "Accepted" : "Rejected");
        return response;
    }
}
