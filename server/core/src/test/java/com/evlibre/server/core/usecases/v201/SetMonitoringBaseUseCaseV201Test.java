package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.MonitoringBase;
import com.evlibre.server.core.domain.v201.dto.GenericDeviceModelStatus;
import com.evlibre.server.core.domain.v201.dto.SetMonitoringBaseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SetMonitoringBaseUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private SetMonitoringBaseUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new SetMonitoringBaseUseCaseV201(commandSender);
    }

    @Test
    void null_monitoring_base_rejected() {
        assertThatThrownBy(() -> useCase.setMonitoringBase(tenantId, station, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("monitoringBase");
    }

    @Test
    void all_monitoring_base_values_use_pascal_case_on_wire() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.setMonitoringBase(tenantId, station, MonitoringBase.ALL).join();
        assertThat(commandSender.commands().get(0).payload()).containsEntry("monitoringBase", "All");

        useCase.setMonitoringBase(tenantId, station, MonitoringBase.FACTORY_DEFAULT).join();
        assertThat(commandSender.commands().get(1).payload()).containsEntry("monitoringBase", "FactoryDefault");

        useCase.setMonitoringBase(tenantId, station, MonitoringBase.HARD_WIRED_ONLY).join();
        assertThat(commandSender.commands().get(2).payload()).containsEntry("monitoringBase", "HardWiredOnly");
    }

    @Test
    void all_four_response_statuses_decoded() {
        for (String wire : new String[] {"Accepted", "Rejected", "NotSupported", "EmptyResultSet"}) {
            commandSender.setNextResponse(Map.of("status", wire));
            SetMonitoringBaseResult r = useCase.setMonitoringBase(tenantId, station, MonitoringBase.ALL).join();
            assertThat(r.status().name()).isEqualTo(toEnumName(wire));
        }
    }

    @Test
    void accepted_marks_isAccepted_true() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        SetMonitoringBaseResult r = useCase.setMonitoringBase(tenantId, station, MonitoringBase.ALL).join();

        assertThat(r.isAccepted()).isTrue();
        assertThat(r.status()).isEqualTo(GenericDeviceModelStatus.ACCEPTED);
    }

    @Test
    void statusInfo_reasonCode_surfaced_on_rejection() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "MonitoringDisabled")));

        SetMonitoringBaseResult r = useCase.setMonitoringBase(tenantId, station, MonitoringBase.ALL).join();

        assertThat(r.isAccepted()).isFalse();
        assertThat(r.statusInfoReason()).isEqualTo("MonitoringDisabled");
    }

    @Test
    void unknown_wire_status_raises() {
        commandSender.setNextResponse(Map.of("status", "FutureStatus"));

        assertThatThrownBy(() -> useCase.setMonitoringBase(tenantId, station, MonitoringBase.ALL).join())
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
