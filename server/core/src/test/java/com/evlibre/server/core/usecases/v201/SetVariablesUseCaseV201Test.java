package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.AttributeType;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.devicemodel.SetVariableData;
import com.evlibre.server.core.domain.v201.devicemodel.SetVariableResult;
import com.evlibre.server.core.domain.v201.devicemodel.SetVariableStatus;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SetVariablesUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private SetVariablesUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new SetVariablesUseCaseV201(commandSender);
    }

    @Test
    void empty_request_list_is_rejected() {
        assertThatThrownBy(() -> useCase.setVariables(tenantId, station, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one");
    }

    @Test
    void attributeValue_over_1000_chars_is_rejected_at_construction() {
        String tooLong = "x".repeat(1001);
        assertThatThrownBy(() -> SetVariableData.of(
                Component.of("C"), Variable.of("V"), tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1000");
    }

    @Test
    void single_entry_serialised_with_required_fields_only() {
        commandSender.setNextResponse(singleResultResponse("Actual", "Accepted"));

        useCase.setVariables(tenantId, station, List.of(
                        SetVariableData.of(Component.of("C"), Variable.of("V"), "newvalue")))
                .join();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wire = (List<Map<String, Object>>)
                commandSender.commands().get(0).payload().get("setVariableData");
        assertThat(wire).hasSize(1);
        Map<String, Object> entry = wire.get(0);
        assertThat(entry)
                .containsEntry("attributeValue", "newvalue")
                .doesNotContainKey("attributeType");
    }

    @Test
    void explicit_attributeType_serialised() {
        commandSender.setNextResponse(singleResultResponse("Target", "Accepted"));

        useCase.setVariables(tenantId, station, List.of(
                        SetVariableData.of(Component.of("C"), Variable.of("V"), "v", AttributeType.TARGET)))
                .join();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wire = (List<Map<String, Object>>)
                commandSender.commands().get(0).payload().get("setVariableData");
        assertThat(wire.get(0)).containsEntry("attributeType", "Target");
    }

    @Test
    void component_with_evse_serialised_fully() {
        commandSender.setNextResponse(singleResultResponse("Actual", "Accepted"));

        useCase.setVariables(tenantId, station, List.of(
                        SetVariableData.of(
                                new Component("EVSE", "primary", Evse.of(1, 2)),
                                new Variable("AvailabilityState", "a"),
                                "Available")))
                .join();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wire = (List<Map<String, Object>>)
                commandSender.commands().get(0).payload().get("setVariableData");
        @SuppressWarnings("unchecked")
        Map<String, Object> component = (Map<String, Object>) wire.get(0).get("component");
        assertThat(component).containsEntry("name", "EVSE").containsEntry("instance", "primary");
        @SuppressWarnings("unchecked")
        Map<String, Object> evse = (Map<String, Object>) component.get("evse");
        assertThat(evse).containsEntry("id", 1).containsEntry("connectorId", 2);
    }

    @Test
    void accepted_result_decoded_with_default_attributeType() {
        Map<String, Object> entry = Map.of(
                "attributeStatus", "Accepted",
                "component", Map.of("name", "C"),
                "variable", Map.of("name", "V"));
        commandSender.setNextResponse(Map.of("setVariableResult", List.of(entry)));

        List<SetVariableResult> results = useCase.setVariables(tenantId, station, List.of(
                        SetVariableData.of(Component.of("C"), Variable.of("V"), "x"))).join();

        assertThat(results).hasSize(1);
        SetVariableResult r = results.get(0);
        assertThat(r.isAccepted()).isTrue();
        assertThat(r.requiresReboot()).isFalse();
        assertThat(r.attributeType()).isEqualTo(AttributeType.ACTUAL);
        assertThat(r.statusInfoReason()).isNull();
    }

    @Test
    void all_six_status_values_decoded_including_reboot_required() {
        Map<String, Object> comp = Map.of("name", "C");
        Map<String, Object> var = Map.of("name", "V");
        Map<String, Object> response = Map.of("setVariableResult", List.of(
                Map.of("attributeStatus", "Accepted", "component", comp, "variable", var),
                Map.of("attributeStatus", "Rejected", "component", comp, "variable", var),
                Map.of("attributeStatus", "UnknownComponent", "component", comp, "variable", var),
                Map.of("attributeStatus", "UnknownVariable", "component", comp, "variable", var),
                Map.of("attributeStatus", "NotSupportedAttributeType", "component", comp, "variable", var),
                Map.of("attributeStatus", "RebootRequired", "component", comp, "variable", var)));
        commandSender.setNextResponse(response);

        List<SetVariableResult> results = useCase.setVariables(tenantId, station, List.of(
                        SetVariableData.of(Component.of("C"), Variable.of("V"), "x"))).join();

        assertThat(results).extracting(SetVariableResult::status)
                .containsExactly(
                        SetVariableStatus.ACCEPTED,
                        SetVariableStatus.REJECTED,
                        SetVariableStatus.UNKNOWN_COMPONENT,
                        SetVariableStatus.UNKNOWN_VARIABLE,
                        SetVariableStatus.NOT_SUPPORTED_ATTRIBUTE_TYPE,
                        SetVariableStatus.REBOOT_REQUIRED);
        assertThat(results.get(5).requiresReboot()).isTrue();
    }

    @Test
    void status_info_reason_propagated() {
        Map<String, Object> entry = Map.of(
                "attributeStatus", "Rejected",
                "attributeStatusInfo", Map.of("reasonCode", "ReadOnly"),
                "component", Map.of("name", "C"),
                "variable", Map.of("name", "V"));
        commandSender.setNextResponse(Map.of("setVariableResult", List.of(entry)));

        List<SetVariableResult> results = useCase.setVariables(tenantId, station, List.of(
                        SetVariableData.of(Component.of("C"), Variable.of("V"), "x"))).join();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo(SetVariableStatus.REJECTED);
        assertThat(results.get(0).statusInfoReason()).isEqualTo("ReadOnly");
    }

    @Test
    void unknown_wire_status_raises() {
        Map<String, Object> entry = Map.of(
                "attributeStatus", "SomeUnknownStatus",
                "component", Map.of("name", "C"),
                "variable", Map.of("name", "V"));
        commandSender.setNextResponse(Map.of("setVariableResult", List.of(entry)));

        assertThatThrownBy(() -> useCase.setVariables(tenantId, station, List.of(
                        SetVariableData.of(Component.of("C"), Variable.of("V"), "x")))
                .join())
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private static Map<String, Object> singleResultResponse(String attrType, String status) {
        return Map.of("setVariableResult", List.of(Map.of(
                "attributeStatus", status,
                "attributeType", attrType,
                "component", Map.of("name", "C"),
                "variable", Map.of("name", "V"))));
    }
}
