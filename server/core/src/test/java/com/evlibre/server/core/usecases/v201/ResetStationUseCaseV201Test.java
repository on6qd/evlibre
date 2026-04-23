package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.model.ResetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResetStationUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private ResetStationUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new ResetStationUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    @Test
    void immediate_whole_station_sends_type_without_evseId() {
        CommandResult result = useCase.reset(tenantId, station, ResetType.IMMEDIATE, null).join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("Reset");
        assertThat(cmd.payload())
                .containsEntry("type", "Immediate")
                .doesNotContainKey("evseId");
    }

    @Test
    void on_idle_maps_to_wire_with_camel_case() {
        useCase.reset(tenantId, station, ResetType.ON_IDLE, null).join();

        assertThat(commandSender.commands().get(0).payload()).containsEntry("type", "OnIdle");
    }

    @Test
    void per_evse_reset_includes_evseId() {
        useCase.reset(tenantId, station, ResetType.IMMEDIATE, 2).join();

        assertThat(commandSender.commands().get(0).payload())
                .containsEntry("type", "Immediate")
                .containsEntry("evseId", 2);
    }

    @Test
    void scheduled_status_is_propagated() {
        commandSender.setNextResponse(Map.of("status", "Scheduled"));

        CommandResult r = useCase.reset(tenantId, station, ResetType.ON_IDLE, null).join();

        assertThat(r.status()).isEqualTo("Scheduled");
        assertThat(r.isAccepted()).isFalse();
    }

    @Test
    void rejected_status_is_propagated() {
        commandSender.setNextResponse(Map.of("status", "Rejected"));

        CommandResult r = useCase.reset(tenantId, station, ResetType.IMMEDIATE, 99).join();

        assertThat(r.status()).isEqualTo("Rejected");
        assertThat(r.isAccepted()).isFalse();
    }

    @Test
    void null_reset_type_rejected() {
        assertThatThrownBy(() -> useCase.reset(tenantId, station, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ResetType");
    }

    @Test
    void negative_evseId_rejected() {
        assertThatThrownBy(() -> useCase.reset(tenantId, station, ResetType.IMMEDIATE, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evseId");
    }
}
