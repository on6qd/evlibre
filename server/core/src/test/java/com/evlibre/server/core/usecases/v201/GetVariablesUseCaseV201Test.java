package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.AttributeType;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.devicemodel.GetVariableData;
import com.evlibre.server.core.domain.v201.devicemodel.GetVariableResult;
import com.evlibre.server.core.domain.v201.devicemodel.GetVariableStatus;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetVariablesUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private GetVariablesUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new GetVariablesUseCaseV201(commandSender);
    }

    @Test
    void empty_request_list_is_rejected() {
        assertThatThrownBy(() -> useCase.getVariables(tenantId, station, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one");
    }

    @Test
    void single_entry_without_attributeType_omits_wire_field() {
        commandSender.setNextResponse(singleResultResponse("Actual", "Accepted", "100"));

        useCase.getVariables(tenantId, station,
                        List.of(GetVariableData.of(Component.of("SecurityCtrlr"), Variable.of("BasicAuthPassword"))))
                .join();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wire = (List<Map<String, Object>>)
                commandSender.commands().get(0).payload().get("getVariableData");
        assertThat(wire).hasSize(1);
        assertThat(wire.get(0)).doesNotContainKey("attributeType");
        @SuppressWarnings("unchecked")
        Map<String, Object> component = (Map<String, Object>) wire.get(0).get("component");
        assertThat(component).containsEntry("name", "SecurityCtrlr");
    }

    @Test
    void explicit_attributeType_maps_all_four_values_to_wire() {
        commandSender.setNextResponse(singleResultResponse("Target", "Accepted", "50"));

        for (AttributeType t : AttributeType.values()) {
            commandSender.commands(); // access to reset pattern — just retrieve
            useCase.getVariables(tenantId, station,
                            List.of(GetVariableData.of(Component.of("C"), Variable.of("V"), t)))
                    .join();
        }

        List<String> seen = commandSender.commands().stream()
                .map(cmd -> {
                    @SuppressWarnings("unchecked")
                    var list = (List<Map<String, Object>>) cmd.payload().get("getVariableData");
                    return (String) list.get(0).get("attributeType");
                })
                .toList();
        assertThat(seen).containsExactly("Actual", "Target", "MinSet", "MaxSet");
    }

    @Test
    void component_with_evse_serialised_fully() {
        commandSender.setNextResponse(singleResultResponse("Actual", "Accepted", "1"));

        useCase.getVariables(tenantId, station,
                        List.of(GetVariableData.of(
                                new Component("EVSE", "primary", Evse.of(1, 2)),
                                new Variable("AvailabilityState", "a"))))
                .join();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wire = (List<Map<String, Object>>)
                commandSender.commands().get(0).payload().get("getVariableData");
        @SuppressWarnings("unchecked")
        Map<String, Object> component = (Map<String, Object>) wire.get(0).get("component");
        assertThat(component).containsEntry("name", "EVSE").containsEntry("instance", "primary");
        @SuppressWarnings("unchecked")
        Map<String, Object> evse = (Map<String, Object>) component.get("evse");
        assertThat(evse).containsEntry("id", 1).containsEntry("connectorId", 2);
    }

    @Test
    void accepted_result_decoded_with_value_and_default_attributeType() {
        Map<String, Object> entry = Map.of(
                "attributeStatus", "Accepted",
                "attributeValue", "42",
                "component", Map.of("name", "SecurityCtrlr"),
                "variable", Map.of("name", "BasicAuthPassword"));
        commandSender.setNextResponse(Map.of("getVariableResult", List.of(entry)));

        List<GetVariableResult> results = useCase.getVariables(tenantId, station,
                        List.of(GetVariableData.of(Component.of("SecurityCtrlr"), Variable.of("BasicAuthPassword"))))
                .join();

        assertThat(results).hasSize(1);
        GetVariableResult r = results.get(0);
        assertThat(r.isAccepted()).isTrue();
        assertThat(r.status()).isEqualTo(GetVariableStatus.ACCEPTED);
        assertThat(r.attributeValue()).isEqualTo("42");
        assertThat(r.attributeType()).isEqualTo(AttributeType.ACTUAL);
        assertThat(r.component().name()).isEqualTo("SecurityCtrlr");
        assertThat(r.variable().name()).isEqualTo("BasicAuthPassword");
        assertThat(r.statusInfoReason()).isNull();
    }

    @Test
    void all_five_status_values_decoded() {
        Map<String, Object> comp = Map.of("name", "C");
        Map<String, Object> var = Map.of("name", "V");
        Map<String, Object> response = Map.of("getVariableResult", List.of(
                Map.of("attributeStatus", "Accepted", "component", comp, "variable", var),
                Map.of("attributeStatus", "Rejected", "component", comp, "variable", var),
                Map.of("attributeStatus", "UnknownComponent", "component", comp, "variable", var),
                Map.of("attributeStatus", "UnknownVariable", "component", comp, "variable", var),
                Map.of("attributeStatus", "NotSupportedAttributeType", "component", comp, "variable", var)));
        commandSender.setNextResponse(response);

        List<GetVariableResult> results = useCase.getVariables(tenantId, station,
                        List.of(GetVariableData.of(Component.of("C"), Variable.of("V"))))
                .join();

        assertThat(results).extracting(GetVariableResult::status)
                .containsExactly(
                        GetVariableStatus.ACCEPTED,
                        GetVariableStatus.REJECTED,
                        GetVariableStatus.UNKNOWN_COMPONENT,
                        GetVariableStatus.UNKNOWN_VARIABLE,
                        GetVariableStatus.NOT_SUPPORTED_ATTRIBUTE_TYPE);
    }

    @Test
    void accepted_with_empty_attributeValue_is_preserved_as_empty_string() {
        // Spec §1.26: an Accepted variable that currently has no assigned value
        // (e.g. a Target not yet set) SHALL return attributeValue as "". The decoder
        // must pass that through — collapsing it to null would make it
        // indistinguishable from a non-Accepted entry whose value is legitimately absent.
        Map<String, Object> entry = Map.of(
                "attributeStatus", "Accepted",
                "attributeValue", "",
                "component", Map.of("name", "C"),
                "variable", Map.of("name", "V"));
        commandSender.setNextResponse(Map.of("getVariableResult", List.of(entry)));

        List<GetVariableResult> results = useCase.getVariables(tenantId, station,
                        List.of(GetVariableData.of(Component.of("C"), Variable.of("V"))))
                .join();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo(GetVariableStatus.ACCEPTED);
        assertThat(results.get(0).attributeValue()).isEqualTo("");
    }

    @Test
    void status_info_reason_propagated_when_present() {
        Map<String, Object> entry = Map.of(
                "attributeStatus", "Rejected",
                "attributeStatusInfo", Map.of("reasonCode", "WriteOnly", "additionalInfo", "not readable"),
                "component", Map.of("name", "C"),
                "variable", Map.of("name", "V"));
        commandSender.setNextResponse(Map.of("getVariableResult", List.of(entry)));

        List<GetVariableResult> results = useCase.getVariables(tenantId, station,
                        List.of(GetVariableData.of(Component.of("C"), Variable.of("V"))))
                .join();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo(GetVariableStatus.REJECTED);
        assertThat(results.get(0).statusInfoReason()).isEqualTo("WriteOnly");
        assertThat(results.get(0).attributeValue()).isNull();
    }

    @Test
    void unknown_wire_status_raises() {
        Map<String, Object> entry = Map.of(
                "attributeStatus", "SomeUnknownStatus",
                "component", Map.of("name", "C"),
                "variable", Map.of("name", "V"));
        commandSender.setNextResponse(Map.of("getVariableResult", List.of(entry)));

        assertThatThrownBy(() -> useCase.getVariables(tenantId, station,
                        List.of(GetVariableData.of(Component.of("C"), Variable.of("V"))))
                .join())
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private static Map<String, Object> singleResultResponse(String attrType, String status, String value) {
        return Map.of("getVariableResult", List.of(Map.of(
                "attributeStatus", status,
                "attributeType", attrType,
                "attributeValue", value,
                "component", Map.of("name", "C"),
                "variable", Map.of("name", "V"))));
    }
}
