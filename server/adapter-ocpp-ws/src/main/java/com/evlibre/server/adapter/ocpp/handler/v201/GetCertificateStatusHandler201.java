package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.dto.GetCertificateStatusResult;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleGetCertificateStatusPort;
import com.evlibre.server.core.domain.v201.security.OcspRequestData;
import com.evlibre.server.core.domain.v201.security.SecurityWire;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetCertificateStatusHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(GetCertificateStatusHandler201.class);

    private final HandleGetCertificateStatusPort port;
    private final ObjectMapper objectMapper;

    public GetCertificateStatusHandler201(HandleGetCertificateStatusPort port, ObjectMapper objectMapper) {
        this.port = port;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        JsonNode ocspNode = payload.path("ocspRequestData");
        OcspRequestData ocsp = new OcspRequestData(
                SecurityWire.hashAlgorithmFromWire(ocspNode.path("hashAlgorithm").asText()),
                ocspNode.path("issuerNameHash").asText(),
                ocspNode.path("issuerKeyHash").asText(),
                ocspNode.path("serialNumber").asText(),
                ocspNode.path("responderURL").asText());

        GetCertificateStatusResult result = port.handleGetCertificateStatus(
                session.tenantId(), session.stationIdentity(), ocsp);

        log.info("GetCertificateStatus from {} (serial={}): {}",
                session.stationIdentity().value(), ocsp.serialNumber(), result.status());

        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", result.isAccepted() ? "Accepted" : "Failed");
        if (result.ocspResult() != null) {
            response.put("ocspResult", result.ocspResult());
        }
        if (result.statusInfoReason() != null) {
            ObjectNode statusInfo = response.putObject("statusInfo");
            statusInfo.put("reasonCode", result.statusInfoReason());
        }
        return response;
    }
}
