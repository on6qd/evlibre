package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;

import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.Reservation;
import com.evlibre.server.core.domain.model.ReservationStatus;
import com.evlibre.server.core.domain.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CancelReservationUseCaseTest {

    private StubCommandSender commandSender;
    private StubReservationRepository reservationRepo;
    private CancelReservationUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        reservationRepo = new StubReservationRepository();
        useCase = new CancelReservationUseCase(commandSender, reservationRepo);
    }

    @Test
    void accepted_cancel_updates_status() {
        reservationRepo.save(Reservation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .stationIdentity(station)
                .connectorId(1)
                .expiryDate(Instant.parse("2025-06-01T12:00:00Z"))
                .idTag("TAG001")
                .reservationId(1)
                .status(ReservationStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());

        commandSender.setNextResponse(Map.of("status", "Accepted"));

        CommandResult result = useCase.cancelReservation(tenantId, station, 1).join();

        assertThat(result.isAccepted()).isTrue();
        var updated = reservationRepo.findByReservationId(tenantId, 1);
        assertThat(updated).isPresent();
        assertThat(updated.get().status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void rejected_cancel_does_not_change_status() {
        reservationRepo.save(Reservation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .stationIdentity(station)
                .connectorId(1)
                .expiryDate(Instant.parse("2025-06-01T12:00:00Z"))
                .idTag("TAG001")
                .reservationId(1)
                .status(ReservationStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());

        commandSender.setNextResponse(Map.of("status", "Rejected"));

        CommandResult result = useCase.cancelReservation(tenantId, station, 1).join();

        assertThat(result.isAccepted()).isFalse();
        assertThat(reservationRepo.findByReservationId(tenantId, 1).get().status())
                .isEqualTo(ReservationStatus.ACTIVE);
    }
}
