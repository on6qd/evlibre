package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.CancelReservationResult;
import com.evlibre.server.core.domain.v201.dto.CancelReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancelReservationUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private CancelReservationUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new CancelReservationUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    @Test
    void payload_carries_reservation_id_only() {
        useCase.cancelReservation(tenantId, station, 42).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("CancelReservation");
        assertThat(cmd.payload())
                .hasSize(1)
                .containsEntry("reservationId", 42);
    }

    @Test
    void accepted_status_parsed() {
        CancelReservationResult r = useCase.cancelReservation(tenantId, station, 1).join();

        assertThat(r.isAccepted()).isTrue();
        assertThat(r.status()).isEqualTo(CancelReservationStatus.ACCEPTED);
    }

    @Test
    void rejected_status_surfaces_status_info_reason() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "NoSuchReservation")));

        CancelReservationResult r = useCase.cancelReservation(tenantId, station, 9).join();

        assertThat(r.isAccepted()).isFalse();
        assertThat(r.status()).isEqualTo(CancelReservationStatus.REJECTED);
        assertThat(r.statusInfoReason()).isEqualTo("NoSuchReservation");
    }

    @Test
    void unknown_wire_status_rejected() {
        commandSender.setNextResponse(Map.of("status", "Maybe"));

        assertThatThrownBy(() -> useCase.cancelReservation(tenantId, station, 1).join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maybe");
    }
}
