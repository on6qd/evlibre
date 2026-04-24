package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.devicemodel.MonitorType;
import com.evlibre.server.core.domain.v201.devicemodel.SetMonitoringData;
import com.evlibre.server.core.domain.v201.devicemodel.SetMonitoringResult;
import com.evlibre.server.core.domain.v201.devicemodel.SetMonitoringStatus;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SetVariableMonitoringUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private SetVariableMonitoringUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new SetVariableMonitoringUseCaseV201(commandSender);
    }

    @Test
    void empty_request_list_is_rejected() {
        assertThatThrownBy(() -> useCase.setVariableMonitoring(tenantId, station, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one");
    }

    @Test
    void severity_out_of_range_rejected_at_construction() {
        assertThatThrownBy(() -> SetMonitoringData.create(
                Component.of("C"), Variable.of("V"), MonitorType.UPPER_THRESHOLD, 10.0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("severity");
    }

    @Test
    void create_monitor_omits_id_and_transaction_fields_on_wire() {
        commandSender.setNextResponse(singleAcceptedResultResponse(42));

        useCase.setVariableMonitoring(tenantId, station, List.of(
                SetMonitoringData.create(Component.of("C"), Variable.of("V"),
                        MonitorType.UPPER_THRESHOLD, 15.5, 3))).join();

        Map<String, Object> entry = firstEntry();
        assertThat(entry)
                .doesNotContainKey("id")
                .doesNotContainKey("transaction")
                .containsEntry("value", 15.5)
                .containsEntry("type", "UpperThreshold")
                .containsEntry("severity", 3);
    }

    @Test
    void replace_monitor_emits_id_field() {
        commandSender.setNextResponse(singleAcceptedResultResponse(7));

        useCase.setVariableMonitoring(tenantId, station, List.of(
                SetMonitoringData.replace(7, Component.of("C"), Variable.of("V"),
                        MonitorType.DELTA, 2.0, 5))).join();

        assertThat(firstEntry()).containsEntry("id", 7);
    }

    @Test
    void transactionOnly_true_emitted_on_wire() {
        commandSender.setNextResponse(singleAcceptedResultResponse(1));

        useCase.setVariableMonitoring(tenantId, station, List.of(
                new SetMonitoringData(null, true, 30.0,
                        MonitorType.PERIODIC, 5,
                        Component.of("C"), Variable.of("V")))).join();

        assertThat(firstEntry()).containsEntry("transaction", true);
    }

    @Test
    void component_with_evse_serialised_fully() {
        commandSender.setNextResponse(singleAcceptedResultResponse(1));

        useCase.setVariableMonitoring(tenantId, station, List.of(
                SetMonitoringData.create(
                        new Component("EVSE", "primary", Evse.of(1, 2)),
                        Variable.of("Power"),
                        MonitorType.UPPER_THRESHOLD, 7400.0, 4))).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> component = (Map<String, Object>) firstEntry().get("component");
        assertThat(component).containsEntry("name", "EVSE").containsEntry("instance", "primary");
        @SuppressWarnings("unchecked")
        Map<String, Object> evse = (Map<String, Object>) component.get("evse");
        assertThat(evse).containsEntry("id", 1).containsEntry("connectorId", 2);
    }

    @Test
    void accepted_result_decoded_with_station_assigned_id() {
        commandSender.setNextResponse(singleAcceptedResultResponse(42));

        List<SetMonitoringResult> results = useCase.setVariableMonitoring(tenantId, station, List.of(
                SetMonitoringData.create(Component.of("C"), Variable.of("V"),
                        MonitorType.UPPER_THRESHOLD, 10.0, 5))).join();

        assertThat(results).hasSize(1);
        SetMonitoringResult r = results.get(0);
        assertThat(r.isAccepted()).isTrue();
        assertThat(r.id()).isEqualTo(42);
        assertThat(r.statusInfoReason()).isNull();
    }

    @Test
    void all_six_status_values_decoded() {
        Map<String, Object> comp = Map.of("name", "C");
        Map<String, Object> var = Map.of("name", "V");
        List<String> wireStatuses = List.of(
                "Accepted", "UnknownComponent", "UnknownVariable",
                "UnsupportedMonitorType", "Rejected", "Duplicate");
        List<Map<String, Object>> items = wireStatuses.stream().map(s -> (Map<String, Object>) Map.of(
                "status", s,
                "type", "Delta",
                "severity", 5,
                "component", comp,
                "variable", var)).toList();
        commandSender.setNextResponse(Map.of("setMonitoringResult", items));

        List<SetMonitoringResult> results = useCase.setVariableMonitoring(tenantId, station, List.of(
                SetMonitoringData.create(Component.of("C"), Variable.of("V"),
                        MonitorType.DELTA, 1.0, 5))).join();

        assertThat(results).extracting(SetMonitoringResult::status)
                .containsExactly(
                        SetMonitoringStatus.ACCEPTED,
                        SetMonitoringStatus.UNKNOWN_COMPONENT,
                        SetMonitoringStatus.UNKNOWN_VARIABLE,
                        SetMonitoringStatus.UNSUPPORTED_MONITOR_TYPE,
                        SetMonitoringStatus.REJECTED,
                        SetMonitoringStatus.DUPLICATE);
    }

    @Test
    void non_accepted_result_has_null_id() {
        Map<String, Object> item = Map.of(
                "status", "Rejected",
                "type", "UpperThreshold",
                "severity", 5,
                "component", Map.of("name", "C"),
                "variable", Map.of("name", "V"));
        commandSender.setNextResponse(Map.of("setMonitoringResult", List.of(item)));

        List<SetMonitoringResult> results = useCase.setVariableMonitoring(tenantId, station, List.of(
                SetMonitoringData.create(Component.of("C"), Variable.of("V"),
                        MonitorType.UPPER_THRESHOLD, 10.0, 5))).join();

        assertThat(results.get(0).id()).isNull();
    }

    @Test
    void status_info_reason_propagated() {
        Map<String, Object> item = Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "MonitoringNotAllowed"),
                "type", "UpperThreshold",
                "severity", 5,
                "component", Map.of("name", "C"),
                "variable", Map.of("name", "V"));
        commandSender.setNextResponse(Map.of("setMonitoringResult", List.of(item)));

        List<SetMonitoringResult> results = useCase.setVariableMonitoring(tenantId, station, List.of(
                SetMonitoringData.create(Component.of("C"), Variable.of("V"),
                        MonitorType.UPPER_THRESHOLD, 10.0, 5))).join();

        assertThat(results.get(0).statusInfoReason()).isEqualTo("MonitoringNotAllowed");
    }

    @Test
    void unknown_wire_status_raises() {
        Map<String, Object> item = Map.of(
                "status", "SomeFutureStatus",
                "type", "UpperThreshold",
                "severity", 5,
                "component", Map.of("name", "C"),
                "variable", Map.of("name", "V"));
        commandSender.setNextResponse(Map.of("setMonitoringResult", List.of(item)));

        assertThatThrownBy(() -> useCase.setVariableMonitoring(tenantId, station, List.of(
                SetMonitoringData.create(Component.of("C"), Variable.of("V"),
                        MonitorType.UPPER_THRESHOLD, 10.0, 5))).join())
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private static Map<String, Object> singleAcceptedResultResponse(int id) {
        return Map.of("setMonitoringResult", List.of(Map.of(
                "id", id,
                "status", "Accepted",
                "type", "UpperThreshold",
                "severity", 5,
                "component", Map.of("name", "C"),
                "variable", Map.of("name", "V"))));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstEntry() {
        List<Map<String, Object>> wire = (List<Map<String, Object>>)
                commandSender.commands().get(0).payload().get("setMonitoringData");
        return wire.get(0);
    }
}
