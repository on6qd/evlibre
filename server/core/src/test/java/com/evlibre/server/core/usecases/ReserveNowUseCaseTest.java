package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;

import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.v16.model.Reservation;
import com.evlibre.server.core.domain.v16.model.ReservationStatus;
import com.evlibre.server.core.domain.shared.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReserveNowUseCaseTest {

    private StubCommandSender commandSender;
    private StubReservationRepository reservationRepo;
    private ReserveNowUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        reservationRepo = new StubReservationRepository();
        useCase = new ReserveNowUseCase(commandSender, reservationRepo);
    }

    @Test
    void accepted_reservation_is_saved() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        CommandResult result = useCase.reserveNow(tenantId, station, 1,
                Instant.parse("2025-06-01T12:00:00Z"), "TAG001").join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("ReserveNow");
        assertThat(cmd.payload()).containsEntry("connectorId", 1);
        assertThat(cmd.payload()).containsEntry("idTag", "TAG001");

        int reservationId = (int) cmd.payload().get("reservationId");
        var saved = reservationRepo.findByReservationId(tenantId, reservationId);
        assertThat(saved).isPresent();
        assertThat(saved.get().status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    // OCPP 1.6 §6: effective status of a reservation whose expiryDate has passed
    // must be EXPIRED regardless of the persisted status.
    @Test
    void effectiveStatus_returns_expired_after_expiry() {
        var r = Reservation.builder()
                .id(java.util.UUID.randomUUID())
                .tenantId(tenantId)
                .stationIdentity(station)
                .connectorId(1)
                .expiryDate(Instant.parse("2020-01-01T00:00:00Z"))
                .idTag("TAG001")
                .reservationId(1)
                .status(ReservationStatus.ACTIVE)
                .createdAt(Instant.parse("2019-12-31T23:00:00Z"))
                .build();

        assertThat(r.status()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(r.effectiveStatus(Instant.parse("2025-01-01T00:00:00Z")))
                .isEqualTo(ReservationStatus.EXPIRED);
        assertThat(r.effectiveStatus(Instant.parse("2019-12-31T23:30:00Z")))
                .isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    void rejected_reservation_is_not_saved() {
        commandSender.setNextResponse(Map.of("status", "Rejected"));

        CommandResult result = useCase.reserveNow(tenantId, station, 1,
                Instant.parse("2025-06-01T12:00:00Z"), "TAG001").join();

        assertThat(result.isAccepted()).isFalse();
        int reservationId = (int) commandSender.commands().get(0).payload().get("reservationId");
        assertThat(reservationRepo.findByReservationId(tenantId, reservationId)).isEmpty();
    }
}
