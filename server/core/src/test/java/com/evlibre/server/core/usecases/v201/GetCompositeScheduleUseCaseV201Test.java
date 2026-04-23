package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GenericStatus;
import com.evlibre.server.core.domain.v201.dto.GetCompositeScheduleResult;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingRateUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetCompositeScheduleUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private GetCompositeScheduleUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new GetCompositeScheduleUseCaseV201(commandSender);
    }

    @Test
    void payload_without_rate_unit_omits_it() {
        commandSender.setNextResponse(Map.of("status", "Rejected"));

        useCase.getCompositeSchedule(tenantId, station, 1, 3600, null).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("GetCompositeSchedule");
        assertThat(cmd.payload())
                .containsEntry("duration", 3600)
                .containsEntry("evseId", 1)
                .doesNotContainKey("chargingRateUnit");
    }

    @Test
    void payload_with_rate_unit_watts_encoded_as_W() {
        commandSender.setNextResponse(Map.of("status", "Rejected"));

        useCase.getCompositeSchedule(tenantId, station, 0, 600, ChargingRateUnit.WATTS).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload())
                .containsEntry("chargingRateUnit", "W")
                .containsEntry("evseId", 0);
    }

    @Test
    void accepted_schedule_parsed_from_wire() {
        commandSender.setNextResponse(Map.of(
                "status", "Accepted",
                "schedule", Map.of(
                        "evseId", 2,
                        "duration", 3600,
                        "scheduleStart", "2027-02-01T10:00:00Z",
                        "chargingRateUnit", "A",
                        "chargingSchedulePeriod", List.of(
                                Map.of("startPeriod", 0, "limit", 32.0),
                                Map.of("startPeriod", 1800, "limit", 16.0, "numberPhases", 1, "phaseToUse", 2)
                        ))));

        GetCompositeScheduleResult r = useCase.getCompositeSchedule(tenantId, station, 2, 3600, ChargingRateUnit.AMPERES).join();

        assertThat(r.isAccepted()).isTrue();
        assertThat(r.status()).isEqualTo(GenericStatus.ACCEPTED);
        assertThat(r.schedule()).isNotNull();
        assertThat(r.schedule().evseId()).isEqualTo(2);
        assertThat(r.schedule().duration()).isEqualTo(3600);
        assertThat(r.schedule().chargingRateUnit()).isEqualTo(ChargingRateUnit.AMPERES);
        assertThat(r.schedule().chargingSchedulePeriod()).hasSize(2);
        var p1 = r.schedule().chargingSchedulePeriod().get(1);
        assertThat(p1.startPeriod()).isEqualTo(1800);
        assertThat(p1.limit()).isEqualTo(16.0);
        assertThat(p1.numberPhases()).isEqualTo(1);
        assertThat(p1.phaseToUse()).isEqualTo(2);
    }

    @Test
    void rejected_no_schedule_surfaces_reason_code() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "UnknownEVSE")));

        GetCompositeScheduleResult r = useCase.getCompositeSchedule(tenantId, station, 99, 3600, null).join();

        assertThat(r.isAccepted()).isFalse();
        assertThat(r.schedule()).isNull();
        assertThat(r.statusInfoReason()).isEqualTo("UnknownEVSE");
    }

    @Test
    void unknown_wire_status_rejected() {
        commandSender.setNextResponse(Map.of("status", "Maybe"));

        assertThatThrownBy(() -> useCase.getCompositeSchedule(tenantId, station, 1, 60, null).join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maybe");
    }

    @Test
    void non_positive_duration_rejected() {
        assertThatThrownBy(() -> useCase.getCompositeSchedule(tenantId, station, 1, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationSeconds");
        assertThatThrownBy(() -> useCase.getCompositeSchedule(tenantId, station, 1, -10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationSeconds");
    }

    @Test
    void negative_evse_id_rejected() {
        assertThatThrownBy(() -> useCase.getCompositeSchedule(tenantId, station, -1, 60, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evseId");
    }
}
