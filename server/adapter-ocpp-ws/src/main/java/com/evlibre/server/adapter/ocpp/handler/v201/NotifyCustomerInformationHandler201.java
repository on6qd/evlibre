package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyCustomerInformationPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotifyCustomerInformationHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyCustomerInformationHandler201.class);

    private final HandleNotifyCustomerInformationPort handler;
    private final ObjectMapper objectMapper;

    public NotifyCustomerInformationHandler201(HandleNotifyCustomerInformationPort handler,
                                                ObjectMapper objectMapper) {
        this.handler = handler;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        int requestId = payload.path("requestId").asInt();
        int seqNo = payload.path("seqNo").asInt();
        boolean tbc = payload.path("tbc").asBoolean(false);
        String data = payload.hasNonNull("data") ? payload.path("data").asText() : "";

        handler.handleFrame(session.tenantId(), session.stationIdentity(),
                requestId, seqNo, tbc, data);

        log.info("NotifyCustomerInformation from {} (requestId={}, seqNo={}, chars={}, tbc={})",
                session.stationIdentity().value(), requestId, seqNo, data.length(), tbc);

        return objectMapper.createObjectNode();
    }
}
