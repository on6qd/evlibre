package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedMonitoring;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import com.evlibre.server.core.domain.v201.devicemodel.VariableMonitor;
import com.evlibre.server.core.domain.v201.devicemodel.wire.DeviceModelWire;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyMonitoringReportPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class NotifyMonitoringReportHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyMonitoringReportHandler201.class);

    private final HandleNotifyMonitoringReportPort handleNotifyMonitoringReport;
    private final ObjectMapper objectMapper;

    public NotifyMonitoringReportHandler201(HandleNotifyMonitoringReportPort handleNotifyMonitoringReport,
                                             ObjectMapper objectMapper) {
        this.handleNotifyMonitoringReport = handleNotifyMonitoringReport;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        int requestId = payload.path("requestId").asInt();
        int seqNo = payload.path("seqNo").asInt();
        boolean tbc = payload.path("tbc").asBoolean(false);
        JsonNode monitorArr = payload.path("monitor");

        List<ReportedMonitoring> reports = new ArrayList<>();
        if (monitorArr.isArray()) {
            for (JsonNode entry : monitorArr) {
                reports.add(toReportedMonitoring(entry));
            }
        }

        handleNotifyMonitoringReport.handleFrame(
                session.tenantId(), session.stationIdentity(),
                requestId, seqNo, tbc, reports);

        log.info("NotifyMonitoringReport from {} (requestId={}, seqNo={}, reports={}, tbc={})",
                session.stationIdentity().value(), requestId, seqNo, reports.size(), tbc);

        return objectMapper.createObjectNode();
    }

    private static ReportedMonitoring toReportedMonitoring(JsonNode entry) {
        Component component = toComponent(entry.path("component"));
        Variable variable = toVariable(entry.path("variable"));
        List<VariableMonitor> monitors = toMonitors(entry.path("variableMonitoring"));
        return new ReportedMonitoring(component, variable, monitors);
    }

    private static Component toComponent(JsonNode node) {
        String name = node.path("name").asText();
        String instance = node.hasNonNull("instance") ? node.path("instance").asText() : null;
        Evse evse = node.hasNonNull("evse") ? toEvse(node.path("evse")) : null;
        return new Component(name, instance, evse);
    }

    private static Evse toEvse(JsonNode node) {
        int id = node.path("id").asInt();
        Integer connectorId = node.hasNonNull("connectorId") ? node.path("connectorId").asInt() : null;
        return new Evse(id, connectorId);
    }

    private static Variable toVariable(JsonNode node) {
        String name = node.path("name").asText();
        String instance = node.hasNonNull("instance") ? node.path("instance").asText() : null;
        return new Variable(name, instance);
    }

    private static List<VariableMonitor> toMonitors(JsonNode node) {
        List<VariableMonitor> out = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode m : node) {
                out.add(new VariableMonitor(
                        m.path("id").asInt(),
                        m.path("transaction").asBoolean(),
                        m.path("value").asDouble(),
                        DeviceModelWire.monitorTypeFromWire(m.path("type").asText()),
                        m.path("severity").asInt()));
            }
        }
        return out;
    }
}
