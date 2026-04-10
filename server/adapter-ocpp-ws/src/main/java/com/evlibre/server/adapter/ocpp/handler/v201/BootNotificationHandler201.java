package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.dto.RegistrationResult;
import com.evlibre.server.core.domain.dto.StationRegistration;
import com.evlibre.server.core.domain.ports.inbound.RegisterStationPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class BootNotificationHandler201 implements OcppMessageHandler {

    private final RegisterStationPort registerStationPort;
    private final ObjectMapper objectMapper;

    public BootNotificationHandler201(RegisterStationPort registerStationPort, ObjectMapper objectMapper) {
        this.registerStationPort = registerStationPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        JsonNode chargingStation = payload.path("chargingStation");

        StationRegistration registration = new StationRegistration(
                session.tenantId(),
                session.stationIdentity(),
                OcppProtocol.OCPP_201,
                chargingStation.path("vendorName").asText(),
                chargingStation.path("model").asText(),
                chargingStation.path("serialNumber").asText(null),
                chargingStation.path("firmwareVersion").asText(null)
        );

        RegistrationResult result = registerStationPort.register(registration);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", result.status().name().substring(0, 1).toUpperCase()
                + result.status().name().substring(1).toLowerCase());
        response.put("currentTime", DateTimeFormatter.ISO_INSTANT
                .format(result.currentTime().atOffset(ZoneOffset.UTC)));
        response.put("interval", result.heartbeatInterval());

        return response;
    }
}
