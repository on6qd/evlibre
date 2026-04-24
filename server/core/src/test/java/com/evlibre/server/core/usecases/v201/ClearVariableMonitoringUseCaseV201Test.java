package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ClearMonitoringResult;
import com.evlibre.server.core.domain.v201.devicemodel.ClearMonitoringStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClearVariableMonitoringUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private ClearVariableMonitoringUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new ClearVariableMonitoringUseCaseV201(commandSender);
    }

    @Test
    void empty_id_list_is_rejected() {
        assertThatThrownBy(() -> useCase.clearVariableMonitoring(tenantId, station, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one");
    }

    @Test
    void null_id_in_list_is_rejected() {
        assertThatThrownBy(() -> useCase.clearVariableMonitoring(tenantId, station, Arrays.asList(1, null, 3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    void ids_serialised_as_array() {
        commandSender.setNextResponse(Map.of("clearMonitoringResult", List.of(
                Map.of("id", 1, "status", "Accepted"),
                Map.of("id", 2, "status", "Accepted"))));

        useCase.clearVariableMonitoring(tenantId, station, List.of(1, 2)).join();

        assertThat(commandSender.commands().get(0).payload())
                .containsEntry("id", List.of(1, 2));
    }

    @Test
    void all_three_status_values_decoded() {
        commandSender.setNextResponse(Map.of("clearMonitoringResult", List.of(
                Map.of("id", 1, "status", "Accepted"),
                Map.of("id", 2, "status", "Rejected"),
                Map.of("id", 3, "status", "NotFound"))));

        List<ClearMonitoringResult> results = useCase
                .clearVariableMonitoring(tenantId, station, List.of(1, 2, 3)).join();

        assertThat(results).extracting(ClearMonitoringResult::status)
                .containsExactly(
                        ClearMonitoringStatus.ACCEPTED,
                        ClearMonitoringStatus.REJECTED,
                        ClearMonitoringStatus.NOT_FOUND);
        assertThat(results.get(0).isAccepted()).isTrue();
        assertThat(results.get(1).isAccepted()).isFalse();
    }

    @Test
    void status_info_reason_propagated() {
        commandSender.setNextResponse(Map.of("clearMonitoringResult", List.of(
                Map.of("id", 5,
                        "status", "Rejected",
                        "statusInfo", Map.of("reasonCode", "Hardwired")))));

        List<ClearMonitoringResult> results = useCase
                .clearVariableMonitoring(tenantId, station, List.of(5)).join();

        assertThat(results.get(0).statusInfoReason()).isEqualTo("Hardwired");
    }

    @Test
    void unknown_wire_status_raises() {
        commandSender.setNextResponse(Map.of("clearMonitoringResult", List.of(
                Map.of("id", 1, "status", "SomeFutureStatus"))));

        assertThatThrownBy(() -> useCase.clearVariableMonitoring(tenantId, station, List.of(1)).join())
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void numeric_id_from_long_deserialiser_normalised() {
        commandSender.setNextResponse(Map.of("clearMonitoringResult", List.of(
                Map.of("id", 7L, "status", "Accepted"))));

        List<ClearMonitoringResult> results = useCase
                .clearVariableMonitoring(tenantId, station, List.of(7)).join();

        assertThat(results.get(0).id()).isEqualTo(7);
    }
}
