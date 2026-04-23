package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.AttributeType;
import com.evlibre.server.core.domain.v201.devicemodel.DataType;
import com.evlibre.server.core.domain.v201.devicemodel.Mutability;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedVariable;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyReportPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotifyReportHandler201Test {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity stationIdentity = new ChargePointIdentity("CS-201");

    private CapturingPort port;
    private NotifyReportHandler201 handler;
    private OcppSession session;

    @BeforeEach
    void setUp() {
        port = new CapturingPort();
        handler = new NotifyReportHandler201(port, objectMapper);
        session = new OcppSession(tenantId, stationIdentity, OcppProtocol.OCPP_201, null);
    }

    @Test
    void parses_minimal_report_and_delegates_to_port() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": 7, "seqNo": 2, "tbc": true,
                  "generatedAt": "2026-04-21T10:00:00Z",
                  "reportData": [{
                    "component": {"name": "SecurityCtrlr"},
                    "variable": {"name": "BasicAuthPassword"},
                    "variableCharacteristics": {"dataType": "string", "supportsMonitoring": false},
                    "variableAttribute": [{"type": "Actual", "value": "secret"}]
                  }]
                }""");

        JsonNode response = handler.handle(session, "msg-1", payload);

        assertThat(response.isObject()).isTrue();
        assertThat(response.size()).isZero();
        assertThat(port.frames).hasSize(1);

        CapturingPort.Frame f = port.frames.get(0);
        assertThat(f.tenantId).isEqualTo(tenantId);
        assertThat(f.stationIdentity).isEqualTo(stationIdentity);
        assertThat(f.requestId).isEqualTo(7);
        assertThat(f.seqNo).isEqualTo(2);
        assertThat(f.tbc).isTrue();
        assertThat(f.reports).hasSize(1);

        ReportedVariable r = f.reports.get(0);
        assertThat(r.component().name()).isEqualTo("SecurityCtrlr");
        assertThat(r.component().evse()).isNull();
        assertThat(r.variable().name()).isEqualTo("BasicAuthPassword");
        assertThat(r.attributes()).hasSize(1);
        assertThat(r.attributes().get(0).type()).isEqualTo(AttributeType.ACTUAL);
        assertThat(r.attributes().get(0).value()).isEqualTo("secret");
        assertThat(r.characteristics().dataType()).isEqualTo(DataType.STRING);
    }

    @Test
    void missing_tbc_defaults_to_false() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": 1, "seqNo": 0, "generatedAt": "2026-04-21T10:00:00Z",
                  "reportData": []
                }""");

        handler.handle(session, "msg-tbc-default", payload);

        assertThat(port.frames.get(0).tbc).isFalse();
    }

    @Test
    void component_evse_is_parsed_when_present() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": 1, "seqNo": 0, "generatedAt": "2026-04-21T10:00:00Z",
                  "reportData": [{
                    "component": {"name": "Connector", "evse": {"id": 2, "connectorId": 1}},
                    "variable": {"name": "Available"},
                    "variableAttribute": [{"type": "Actual", "value": "true"}]
                  }]
                }""");

        handler.handle(session, "msg-2", payload);

        var component = port.frames.get(0).reports.get(0).component();
        assertThat(component.evse().id()).isEqualTo(2);
        assertThat(component.evse().connectorId()).isEqualTo(1);
    }

    @Test
    void missing_attribute_type_defaults_to_Actual() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": 1, "seqNo": 0, "generatedAt": "2026-04-21T10:00:00Z",
                  "reportData": [{
                    "component": {"name": "EVSE"},
                    "variable": {"name": "Current"},
                    "variableAttribute": [{"value": "16"}]
                  }]
                }""");

        handler.handle(session, "msg-3", payload);

        assertThat(port.frames.get(0).reports.get(0).attributes().get(0).type()).isEqualTo(AttributeType.ACTUAL);
    }

    @Test
    void missing_mutability_defaults_to_ReadWrite() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": 1, "seqNo": 0, "generatedAt": "2026-04-21T10:00:00Z",
                  "reportData": [{
                    "component": {"name": "EVSE"},
                    "variable": {"name": "Current"},
                    "variableAttribute": [{"value": "16"}]
                  }]
                }""");

        handler.handle(session, "msg-4", payload);

        assertThat(port.frames.get(0).reports.get(0).attributes().get(0).mutability())
                .isEqualTo(Mutability.READ_WRITE);
    }

    @Test
    void readOnly_attribute_with_missing_value_does_not_crash() throws Exception {
        // Regression: VariableAttribute.value is optional on the wire for any
        // mutability per spec §2.41. A compliant ReadOnly attribute with no
        // currently-assigned value must not crash the handler.
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": 1, "seqNo": 0, "generatedAt": "2026-04-21T10:00:00Z",
                  "reportData": [{
                    "component": {"name": "EVSE"},
                    "variable": {"name": "CableLength"},
                    "variableAttribute": [{"type": "Actual", "mutability": "ReadOnly"}]
                  }]
                }""");

        handler.handle(session, "msg-readonly-null", payload);

        var attr = port.frames.get(0).reports.get(0).attributes().get(0);
        assertThat(attr.value()).isNull();
        assertThat(attr.mutability()).isEqualTo(Mutability.READ_ONLY);
    }

    @Test
    void missing_characteristics_yields_null() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": 1, "seqNo": 0, "generatedAt": "2026-04-21T10:00:00Z",
                  "reportData": [{
                    "component": {"name": "EVSE"},
                    "variable": {"name": "Current"},
                    "variableAttribute": [{"type": "Actual", "value": "16"}]
                  }]
                }""");

        handler.handle(session, "msg-5", payload);

        assertThat(port.frames.get(0).reports.get(0).characteristics()).isNull();
    }

    @Test
    void wire_enum_variants_map_correctly() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": 1, "seqNo": 0, "generatedAt": "2026-04-21T10:00:00Z",
                  "reportData": [
                    {
                      "component": {"name": "A"}, "variable": {"name": "x"},
                      "variableAttribute": [{"type": "MinSet", "value": "0", "mutability": "ReadOnly"}],
                      "variableCharacteristics": {"dataType": "dateTime", "supportsMonitoring": true}
                    },
                    {
                      "component": {"name": "B"}, "variable": {"name": "y"},
                      "variableAttribute": [{"type": "MaxSet", "value": "10", "mutability": "WriteOnly"}],
                      "variableCharacteristics": {"dataType": "OptionList", "supportsMonitoring": false}
                    }
                  ]
                }""");

        handler.handle(session, "msg-6", payload);

        List<ReportedVariable> reports = port.frames.get(0).reports;
        assertThat(reports).hasSize(2);
        assertThat(reports.get(0).attributes().get(0).type()).isEqualTo(AttributeType.MIN_SET);
        assertThat(reports.get(0).attributes().get(0).mutability()).isEqualTo(Mutability.READ_ONLY);
        assertThat(reports.get(0).characteristics().dataType()).isEqualTo(DataType.DATE_TIME);
        assertThat(reports.get(1).attributes().get(0).type()).isEqualTo(AttributeType.MAX_SET);
        assertThat(reports.get(1).attributes().get(0).mutability()).isEqualTo(Mutability.WRITE_ONLY);
        assertThat(reports.get(1).characteristics().dataType()).isEqualTo(DataType.OPTION_LIST);
    }

    @Test
    void unknown_wire_enum_throws() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": 1, "seqNo": 0, "generatedAt": "2026-04-21T10:00:00Z",
                  "reportData": [{
                    "component": {"name": "EVSE"}, "variable": {"name": "x"},
                    "variableAttribute": [{"type": "Bogus", "value": "1"}]
                  }]
                }""");

        assertThatThrownBy(() -> handler.handle(session, "msg-7", payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bogus");
    }

    @Test
    void empty_reportData_still_forwards_frame_to_port() throws Exception {
        // The port must see every frame (including empty ones) so it can track
        // seqNo progression and fire completion on the tbc=false marker.
        JsonNode payload = objectMapper.readTree("""
                {"requestId": 1, "seqNo": 0, "generatedAt": "2026-04-21T10:00:00Z", "reportData": []}
                """);

        handler.handle(session, "msg-8", payload);

        assertThat(port.frames).hasSize(1);
        assertThat(port.frames.get(0).reports).isEmpty();
    }

    private static final class CapturingPort implements HandleNotifyReportPort {
        record Frame(TenantId tenantId, ChargePointIdentity stationIdentity,
                     int requestId, int seqNo, boolean tbc,
                     List<ReportedVariable> reports) {}

        final List<Frame> frames = new ArrayList<>();

        @Override
        public void handleFrame(TenantId tenantId, ChargePointIdentity stationIdentity,
                                int requestId, int seqNo, boolean tbc, List<ReportedVariable> reports) {
            frames.add(new Frame(tenantId, stationIdentity, requestId, seqNo, tbc, List.copyOf(reports)));
        }
    }
}
