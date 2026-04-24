package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.dto.SignCertificateResult;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleSignCertificatePort;
import com.evlibre.server.core.domain.v201.security.CertificateSigningUse;
import com.evlibre.server.core.domain.v201.security.SecurityWire;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignCertificateHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(SignCertificateHandler201.class);

    private final HandleSignCertificatePort port;
    private final ObjectMapper objectMapper;

    public SignCertificateHandler201(HandleSignCertificatePort port, ObjectMapper objectMapper) {
        this.port = port;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        String csr = payload.path("csr").asText();
        CertificateSigningUse certificateType = payload.hasNonNull("certificateType")
                ? SecurityWire.certificateSigningUseFromWire(payload.path("certificateType").asText())
                : null;

        SignCertificateResult result = port.handleSignCertificate(
                session.tenantId(), session.stationIdentity(), csr, certificateType);

        log.info("SignCertificate from {} (certificateType={}): {}",
                session.stationIdentity().value(),
                certificateType != null ? certificateType : "default",
                result.status());

        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", result.isAccepted() ? "Accepted" : "Rejected");
        if (result.statusInfoReason() != null) {
            ObjectNode statusInfo = response.putObject("statusInfo");
            statusInfo.put("reasonCode", result.statusInfoReason());
        }
        return response;
    }
}
