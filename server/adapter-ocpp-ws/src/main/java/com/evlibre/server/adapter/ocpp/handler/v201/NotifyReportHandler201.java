package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.v201.devicemodel.AttributeType;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.DataType;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.devicemodel.Mutability;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedVariable;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import com.evlibre.server.core.domain.v201.devicemodel.VariableAttribute;
import com.evlibre.server.core.domain.v201.devicemodel.VariableCharacteristics;
import com.evlibre.server.core.domain.v201.devicemodel.wire.DeviceModelWire;
import com.evlibre.server.core.domain.v201.ports.outbound.DeviceModelRepositoryPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NotifyReportHandler201 implements OcppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyReportHandler201.class);

    private final DeviceModelRepositoryPort deviceModelRepository;
    private final ObjectMapper objectMapper;

    public NotifyReportHandler201(DeviceModelRepositoryPort deviceModelRepository, ObjectMapper objectMapper) {
        this.deviceModelRepository = deviceModelRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(OcppSession session, String messageId, JsonNode payload) {
        int requestId = payload.path("requestId").asInt();
        int seqNo = payload.path("seqNo").asInt();
        boolean tbc = payload.path("tbc").asBoolean(false);
        JsonNode reportData = payload.path("reportData");

        List<ReportedVariable> reports = new ArrayList<>();
        if (reportData.isArray()) {
            for (JsonNode entry : reportData) {
                reports.add(toReportedVariable(entry));
            }
        }

        if (!reports.isEmpty()) {
            deviceModelRepository.upsert(session.tenantId(), session.stationIdentity(), reports);
        }

        log.info("NotifyReport from {} (requestId={}, seqNo={}, reports={}, tbc={})",
                session.stationIdentity().value(), requestId, seqNo, reports.size(), tbc);

        return objectMapper.createObjectNode();
    }

    private static ReportedVariable toReportedVariable(JsonNode entry) {
        Component component = toComponent(entry.path("component"));
        Variable variable = toVariable(entry.path("variable"));
        List<VariableAttribute> attributes = toAttributes(entry.path("variableAttribute"));
        VariableCharacteristics characteristics = entry.hasNonNull("variableCharacteristics")
                ? toCharacteristics(entry.path("variableCharacteristics"))
                : null;

        return new ReportedVariable(component, variable, attributes, characteristics);
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

    private static List<VariableAttribute> toAttributes(JsonNode node) {
        List<VariableAttribute> out = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode attr : node) {
                AttributeType type = attr.hasNonNull("type")
                        ? DeviceModelWire.attributeTypeFromWire(attr.path("type").asText())
                        : AttributeType.DEFAULT;
                String value = attr.hasNonNull("value") ? attr.path("value").asText() : null;
                Mutability mutability = attr.hasNonNull("mutability")
                        ? mutabilityFromWire(attr.path("mutability").asText())
                        : Mutability.DEFAULT;
                boolean persistent = attr.path("persistent").asBoolean(false);
                boolean constant = attr.path("constant").asBoolean(false);
                out.add(new VariableAttribute(type, value, mutability, persistent, constant));
            }
        }
        return out;
    }

    private static VariableCharacteristics toCharacteristics(JsonNode node) {
        String unit = node.hasNonNull("unit") ? node.path("unit").asText() : null;
        DataType dataType = dataTypeFromWire(node.path("dataType").asText());
        BigDecimal minLimit = node.hasNonNull("minLimit") ? node.path("minLimit").decimalValue() : null;
        BigDecimal maxLimit = node.hasNonNull("maxLimit") ? node.path("maxLimit").decimalValue() : null;
        String valuesList = node.hasNonNull("valuesList") ? node.path("valuesList").asText() : null;
        boolean supportsMonitoring = node.path("supportsMonitoring").asBoolean(false);
        return new VariableCharacteristics(unit, dataType, minLimit, maxLimit, valuesList, supportsMonitoring);
    }

    private static Mutability mutabilityFromWire(String wire) {
        return switch (wire) {
            case "ReadOnly" -> Mutability.READ_ONLY;
            case "WriteOnly" -> Mutability.WRITE_ONLY;
            case "ReadWrite" -> Mutability.READ_WRITE;
            default -> throw new IllegalArgumentException("Unknown MutabilityEnumType: " + wire);
        };
    }

    private static DataType dataTypeFromWire(String wire) {
        return switch (wire) {
            case "string" -> DataType.STRING;
            case "decimal" -> DataType.DECIMAL;
            case "integer" -> DataType.INTEGER;
            case "dateTime" -> DataType.DATE_TIME;
            case "boolean" -> DataType.BOOLEAN;
            case "OptionList" -> DataType.OPTION_LIST;
            case "SequenceList" -> DataType.SEQUENCE_LIST;
            case "MemberList" -> DataType.MEMBER_LIST;
            default -> throw new IllegalArgumentException("Unknown DataEnumType: " + wire);
        };
    }
}
