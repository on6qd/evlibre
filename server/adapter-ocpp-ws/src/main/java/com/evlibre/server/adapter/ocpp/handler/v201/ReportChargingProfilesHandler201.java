package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleReportChargingProfilesPort;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfile;
import com.evlibre.server.core.domain.v201.smartcharging.wire.ChargingProfileWire;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportChargingProfilesHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ReportChargingProfilesHandler201.class);

    private final HandleReportChargingProfilesPort handleReport;
    private final ObjectMapper objectMapper;

    public ReportChargingProfilesHandler201(HandleReportChargingProfilesPort handleReport, ObjectMapper objectMapper) {
        this.handleReport = handleReport;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        int requestId = payload.path("requestId").asInt();
        int evseId = payload.path("evseId").asInt();
        boolean tbc = payload.path("tbc").asBoolean(false);
        ChargingLimitSource source = ChargingProfileWire.limitSourceFromWire(
                payload.path("chargingLimitSource").asText());

        List<ChargingProfile> profiles = new ArrayList<>();
        JsonNode profilesNode = payload.path("chargingProfile");
        if (profilesNode.isArray()) {
            for (JsonNode p : profilesNode) {
                @SuppressWarnings("unchecked")
                Map<String, Object> asMap = objectMapper.convertValue(p, Map.class);
                profiles.add(ChargingProfileWire.chargingProfileFromWire(asMap));
            }
        }

        handleReport.handleFrame(
                session.tenantId(), session.stationIdentity(),
                requestId, source, evseId, profiles, tbc);

        log.info("ReportChargingProfiles from {} (requestId={}, evseId={}, source={}, profiles={}, tbc={})",
                session.stationIdentity().value(), requestId, evseId, source, profiles.size(), tbc);

        return objectMapper.createObjectNode();
    }
}
