package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.ComponentVariableSelector;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.devicemodel.MonitoringCriterion;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import com.evlibre.server.core.domain.v201.dto.GenericDeviceModelStatus;
import com.evlibre.server.core.domain.v201.dto.GetMonitoringReportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetMonitoringReportUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private GetMonitoringReportUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new GetMonitoringReportUseCaseV201(commandSender);
    }

    @Test
    void empty_filters_omit_optional_keys() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.getMonitoringReport(tenantId, station, 42, Set.of(), List.of()).join();

        Map<String, Object> payload = commandSender.commands().get(0).payload();
        assertThat(payload)
                .containsEntry("requestId", 42)
                .doesNotContainKey("monitoringCriteria")
                .doesNotContainKey("componentVariable");
    }

    @Test
    void all_three_criteria_use_pascal_case_on_wire() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.getMonitoringReport(tenantId, station, 1,
                EnumSet.allOf(MonitoringCriterion.class), List.of()).join();

        @SuppressWarnings("unchecked")
        List<String> criteria = (List<String>) commandSender.commands().get(0).payload()
                .get("monitoringCriteria");
        assertThat(criteria).containsExactlyInAnyOrder(
                "ThresholdMonitoring", "DeltaMonitoring", "PeriodicMonitoring");
    }

    @Test
    void selectors_include_component_and_optional_variable() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.getMonitoringReport(tenantId, station, 1,
                Set.of(),
                List.of(
                        ComponentVariableSelector.of(
                                new Component("EVSE", "primary", Evse.of(1, 2)),
                                new Variable("Power", null)),
                        ComponentVariableSelector.of(Component.of("ConnectorCtrlr"))))
                .join();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wire = (List<Map<String, Object>>) commandSender.commands().get(0)
                .payload().get("componentVariable");
        assertThat(wire).hasSize(2);
        assertThat(wire.get(0)).containsKey("component").containsKey("variable");
        assertThat(wire.get(1)).containsKey("component").doesNotContainKey("variable");
    }

    @Test
    void all_four_response_statuses_decoded() {
        for (String wire : new String[] {"Accepted", "Rejected", "NotSupported", "EmptyResultSet"}) {
            commandSender.setNextResponse(Map.of("status", wire));
            GetMonitoringReportResult r = useCase
                    .getMonitoringReport(tenantId, station, 1, Set.of(), List.of()).join();
            assertThat(r.status().name()).isEqualTo(toEnumName(wire));
        }
    }

    @Test
    void accepted_marks_isAccepted_true() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        GetMonitoringReportResult r = useCase
                .getMonitoringReport(tenantId, station, 1, Set.of(), List.of()).join();

        assertThat(r.isAccepted()).isTrue();
        assertThat(r.status()).isEqualTo(GenericDeviceModelStatus.ACCEPTED);
    }

    @Test
    void empty_result_set_is_not_accepted() {
        commandSender.setNextResponse(Map.of("status", "EmptyResultSet"));

        GetMonitoringReportResult r = useCase
                .getMonitoringReport(tenantId, station, 1, Set.of(), List.of()).join();

        assertThat(r.isAccepted()).isFalse();
        assertThat(r.status()).isEqualTo(GenericDeviceModelStatus.EMPTY_RESULT_SET);
    }

    @Test
    void statusInfo_reasonCode_surfaced() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "MonitoringDisabled")));

        GetMonitoringReportResult r = useCase
                .getMonitoringReport(tenantId, station, 1, Set.of(), List.of()).join();

        assertThat(r.statusInfoReason()).isEqualTo("MonitoringDisabled");
    }

    @Test
    void unknown_wire_status_raises() {
        commandSender.setNextResponse(Map.of("status", "FutureStatus"));

        assertThatThrownBy(() -> useCase.getMonitoringReport(tenantId, station, 1,
                Set.of(), List.of()).join())
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private static String toEnumName(String wire) {
        return switch (wire) {
            case "Accepted" -> "ACCEPTED";
            case "Rejected" -> "REJECTED";
            case "NotSupported" -> "NOT_SUPPORTED";
            case "EmptyResultSet" -> "EMPTY_RESULT_SET";
            default -> throw new IllegalArgumentException(wire);
        };
    }
}
