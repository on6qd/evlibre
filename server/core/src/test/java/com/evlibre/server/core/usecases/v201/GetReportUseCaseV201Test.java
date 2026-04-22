package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.ComponentCriterion;
import com.evlibre.server.core.domain.v201.devicemodel.ComponentVariableSelector;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GetReportUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private GetReportUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new GetReportUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    @Test
    void empty_filters_send_only_requestId() {
        CommandResult result = useCase.getReport(tenantId, station, 7, Set.of(), List.of()).join();

        assertThat(result.isAccepted()).isTrue();
        assertThat(commandSender.commands()).hasSize(1);
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("GetReport");
        assertThat(cmd.payload())
                .containsEntry("requestId", 7)
                .doesNotContainKey("componentCriteria")
                .doesNotContainKey("componentVariable");
    }

    @Test
    void all_four_criteria_map_to_wire_form() {
        useCase.getReport(tenantId, station, 1,
                EnumSet.allOf(ComponentCriterion.class), List.of()).join();

        @SuppressWarnings("unchecked")
        List<String> wire = (List<String>) commandSender.commands().get(0).payload().get("componentCriteria");
        assertThat(wire).containsExactlyInAnyOrder("Active", "Available", "Enabled", "Problem");
    }

    @Test
    void single_criterion_serialized_as_one_element_array() {
        useCase.getReport(tenantId, station, 1,
                Set.of(ComponentCriterion.PROBLEM), List.of()).join();

        @SuppressWarnings("unchecked")
        List<String> wire = (List<String>) commandSender.commands().get(0).payload().get("componentCriteria");
        assertThat(wire).containsExactly("Problem");
    }

    @Test
    void component_only_selector_omits_variable_on_wire() {
        var selector = ComponentVariableSelector.of(Component.of("SecurityCtrlr"));

        useCase.getReport(tenantId, station, 1, Set.of(), List.of(selector)).join();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wire = (List<Map<String, Object>>)
                commandSender.commands().get(0).payload().get("componentVariable");
        assertThat(wire).hasSize(1);
        Map<String, Object> entry = wire.get(0);
        assertThat(entry).containsKey("component").doesNotContainKey("variable");
        @SuppressWarnings("unchecked")
        Map<String, Object> component = (Map<String, Object>) entry.get("component");
        assertThat(component).containsEntry("name", "SecurityCtrlr").doesNotContainKey("instance").doesNotContainKey("evse");
    }

    @Test
    void component_with_evse_and_variable_with_instance_serialized_fully() {
        var selector = ComponentVariableSelector.of(
                new Component("EVSE", "primary", Evse.of(1, 2)),
                new Variable("AvailabilityState", "A"));

        useCase.getReport(tenantId, station, 1, Set.of(), List.of(selector)).join();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wire = (List<Map<String, Object>>)
                commandSender.commands().get(0).payload().get("componentVariable");
        @SuppressWarnings("unchecked")
        Map<String, Object> component = (Map<String, Object>) wire.get(0).get("component");
        assertThat(component)
                .containsEntry("name", "EVSE")
                .containsEntry("instance", "primary");
        @SuppressWarnings("unchecked")
        Map<String, Object> evse = (Map<String, Object>) component.get("evse");
        assertThat(evse)
                .containsEntry("id", 1)
                .containsEntry("connectorId", 2);
        @SuppressWarnings("unchecked")
        Map<String, Object> variable = (Map<String, Object>) wire.get(0).get("variable");
        assertThat(variable)
                .containsEntry("name", "AvailabilityState")
                .containsEntry("instance", "A");
    }

    @Test
    void rejected_response_is_propagated() {
        commandSender.setNextResponse(Map.of("status", "Rejected"));

        CommandResult result = useCase.getReport(tenantId, station, 1,
                Set.of(ComponentCriterion.ACTIVE), List.of()).join();

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.status()).isEqualTo("Rejected");
    }

    @Test
    void empty_result_set_status_is_propagated() {
        commandSender.setNextResponse(Map.of("status", "EmptyResultSet"));

        CommandResult result = useCase.getReport(tenantId, station, 1,
                Set.of(ComponentCriterion.ENABLED), List.of()).join();

        assertThat(result.status()).isEqualTo("EmptyResultSet");
        assertThat(result.isAccepted()).isFalse();
    }
}
