package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GenericStatus;
import com.evlibre.server.core.domain.v201.dto.SetMonitoringLevelResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SetMonitoringLevelUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private SetMonitoringLevelUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new SetMonitoringLevelUseCaseV201(commandSender);
    }

    @Test
    void severity_below_range_rejected() {
        assertThatThrownBy(() -> useCase.setMonitoringLevel(tenantId, station, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("severity");
    }

    @Test
    void severity_above_range_rejected() {
        assertThatThrownBy(() -> useCase.setMonitoringLevel(tenantId, station, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("severity");
    }

    @Test
    void severity_at_both_boundaries_accepted() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.setMonitoringLevel(tenantId, station, 0).join();
        useCase.setMonitoringLevel(tenantId, station, 9).join();

        assertThat(commandSender.commands().get(0).payload()).containsEntry("severity", 0);
        assertThat(commandSender.commands().get(1).payload()).containsEntry("severity", 9);
    }

    @Test
    void both_response_statuses_decoded() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));
        assertThat(useCase.setMonitoringLevel(tenantId, station, 5).join().status())
                .isEqualTo(GenericStatus.ACCEPTED);

        commandSender.setNextResponse(Map.of("status", "Rejected"));
        assertThat(useCase.setMonitoringLevel(tenantId, station, 5).join().status())
                .isEqualTo(GenericStatus.REJECTED);
    }

    @Test
    void statusInfo_reasonCode_surfaced_on_rejection() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "MonitoringDisabled")));

        SetMonitoringLevelResult r = useCase.setMonitoringLevel(tenantId, station, 5).join();

        assertThat(r.isAccepted()).isFalse();
        assertThat(r.statusInfoReason()).isEqualTo("MonitoringDisabled");
    }

    @Test
    void unknown_wire_status_raises() {
        commandSender.setNextResponse(Map.of("status", "FutureStatus"));

        assertThatThrownBy(() -> useCase.setMonitoringLevel(tenantId, station, 5).join())
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
