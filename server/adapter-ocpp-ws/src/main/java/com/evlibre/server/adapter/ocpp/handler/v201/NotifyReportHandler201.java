package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.model.DeviceModelVariable;
import com.evlibre.server.core.domain.v201.ports.outbound.DeviceModelPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class NotifyReportHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyReportHandler201.class);

    private final DeviceModelPort deviceModelPort;
    private final ObjectMapper objectMapper;

    public NotifyReportHandler201(DeviceModelPort deviceModelPort, ObjectMapper objectMapper) {
        this.deviceModelPort = deviceModelPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        int requestId = payload.path("requestId").asInt();
        int seqNo = payload.path("seqNo").asInt();
        boolean tbc = payload.path("tbc").asBoolean(false);
        JsonNode reportData = payload.path("reportData");

        List<DeviceModelVariable> variables = new ArrayList<>();

        if (reportData.isArray()) {
            for (JsonNode entry : reportData) {
                JsonNode component = entry.path("component");
                JsonNode variable = entry.path("variable");
                JsonNode characteristics = entry.path("variableCharacteristics");

                String componentName = component.path("name").asText();
                String componentInstance = component.path("instance").asText(null);

                // EVSE info (optional)
                Integer evseId = null;
                Integer connectorId = null;
                JsonNode evseNode = component.path("evse");
                if (!evseNode.isMissingNode()) {
                    evseId = evseNode.path("id").asInt();
                    if (evseNode.has("connectorId")) {
                        connectorId = evseNode.path("connectorId").asInt();
                    }
                }

                String variableName = variable.path("name").asText();
                String variableInstance = variable.path("instance").asText(null);

                String dataType = characteristics.path("dataType").asText(null);
                boolean supportsMonitoring = characteristics.path("supportsMonitoring").asBoolean(false);

                // Each reportData entry can have multiple variableAttributes
                JsonNode attrs = entry.path("variableAttribute");
                if (attrs.isArray()) {
                    for (JsonNode attr : attrs) {
                        String attrType = attr.path("type").asText("Actual");
                        String value = attr.path("value").asText(null);

                        variables.add(new DeviceModelVariable(
                                componentName, componentInstance,
                                evseId, connectorId,
                                variableName, variableInstance,
                                attrType, value,
                                dataType, supportsMonitoring
                        ));
                    }
                }
            }
        }

        if (!variables.isEmpty()) {
            deviceModelPort.saveVariables(session.tenantId(), session.stationIdentity(), variables);
        }

        log.info("NotifyReport from {} (requestId={}, seqNo={}, variables={}, tbc={})",
                session.stationIdentity().value(), requestId, seqNo, variables.size(), tbc);

        return objectMapper.createObjectNode();
    }
}
