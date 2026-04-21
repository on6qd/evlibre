package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.OcppSessionManager;
import com.evlibre.server.adapter.ocpp.PostBootActionService;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.shared.dto.RegistrationResult;
import com.evlibre.server.core.domain.shared.dto.StationRegistration;
import com.evlibre.server.core.domain.shared.model.RegistrationStatus;
import com.evlibre.server.core.domain.ports.inbound.RegisterStationPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class BootNotificationHandler201 implements OcppMessageHandler {

    private final RegisterStationPort registerStationPort;
    private final PostBootActionService postBootActionService;
    private final OcppSessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private volatile boolean acceptedOrPending;
    private volatile boolean accepted;

    public BootNotificationHandler201(RegisterStationPort registerStationPort,
                                      PostBootActionService postBootActionService,
                                      ObjectMapper objectMapper) {
        this(registerStationPort, postBootActionService, null, objectMapper);
    }

    public BootNotificationHandler201(RegisterStationPort registerStationPort,
                                      PostBootActionService postBootActionService,
                                      OcppSessionManager sessionManager,
                                      ObjectMapper objectMapper) {
        this.registerStationPort = registerStationPort;
        this.postBootActionService = postBootActionService;
        this.sessionManager = sessionManager;
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
        accepted = result.status() == RegistrationStatus.ACCEPTED;
        acceptedOrPending = result.status() != RegistrationStatus.REJECTED;

        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", result.status().name().substring(0, 1).toUpperCase()
                + result.status().name().substring(1).toLowerCase());
        response.put("currentTime", DateTimeFormatter.ISO_INSTANT
                .format(result.currentTime().atOffset(ZoneOffset.UTC)));
        response.put("interval", result.heartbeatInterval());

        return response;
    }

    @Override
    public void afterResponse(OcppSession session) {
        if (acceptedOrPending && sessionManager != null) {
            sessionManager.markAccepted(session.tenantId(), session.stationIdentity());
        }
        if (accepted && postBootActionService != null) {
            postBootActionService.onBootAccepted(session.tenantId(), session.stationIdentity(),
                    session.protocol());
        }
    }
}
