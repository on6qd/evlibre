package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.dto.Get15118EVCertificateResult;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleGet15118EVCertificatePort;
import com.evlibre.server.core.domain.v201.security.CertificateAction;
import com.evlibre.server.core.domain.v201.security.SecurityWire;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Get15118EVCertificateHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(Get15118EVCertificateHandler201.class);

    private final HandleGet15118EVCertificatePort port;
    private final ObjectMapper objectMapper;

    public Get15118EVCertificateHandler201(HandleGet15118EVCertificatePort port, ObjectMapper objectMapper) {
        this.port = port;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        String schemaVersion = payload.path("iso15118SchemaVersion").asText();
        CertificateAction action = SecurityWire.certificateActionFromWire(
                payload.path("action").asText());
        String exiRequest = payload.path("exiRequest").asText();

        Get15118EVCertificateResult result = port.handleGet15118EVCertificate(
                session.tenantId(), session.stationIdentity(),
                schemaVersion, action, exiRequest);

        log.info("Get15118EVCertificate from {} (action={}): {}",
                session.stationIdentity().value(), action, result.status());

        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", result.isAccepted() ? "Accepted" : "Failed");
        response.put("exiResponse", result.exiResponse());
        if (result.statusInfoReason() != null) {
            ObjectNode statusInfo = response.putObject("statusInfo");
            statusInfo.put("reasonCode", result.statusInfoReason());
        }
        return response;
    }
}
