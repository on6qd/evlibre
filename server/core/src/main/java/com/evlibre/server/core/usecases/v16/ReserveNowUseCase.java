package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.v16.model.Reservation;
import com.evlibre.server.core.domain.v16.model.ReservationStatus;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.inbound.ReserveNowPort;
import com.evlibre.server.core.domain.v16.ports.outbound.ReservationRepositoryPort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ReserveNowUseCase implements ReserveNowPort {

    private static final Logger log = LoggerFactory.getLogger(ReserveNowUseCase.class);

    private final StationCommandSender commandSender;
    private final ReservationRepositoryPort reservationRepo;

    public ReserveNowUseCase(StationCommandSender commandSender, ReservationRepositoryPort reservationRepo) {
        this.commandSender = Objects.requireNonNull(commandSender);
        this.reservationRepo = Objects.requireNonNull(reservationRepo);
    }

    @Override
    public CompletableFuture<CommandResult> reserveNow(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                         int connectorId, Instant expiryDate, String idTag) {
        int reservationId = reservationRepo.nextReservationId();
        log.info("Sending ReserveNow to {} (tenant: {}, reservationId: {})",
                stationIdentity.value(), tenantId.value(), reservationId);

        Map<String, Object> payload = Map.of(
                "connectorId", connectorId,
                "expiryDate", expiryDate.toString(),
                "idTag", idTag,
                "reservationId", reservationId
        );

        return commandSender.sendCommand(tenantId, stationIdentity, "ReserveNow", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("ReserveNow response from {}: {}", stationIdentity.value(), status);

                    if ("Accepted".equals(status)) {
                        Reservation reservation = Reservation.builder()
                                .id(UUID.randomUUID())
                                .tenantId(tenantId)
                                .stationIdentity(stationIdentity)
                                .connectorId(connectorId)
                                .expiryDate(expiryDate)
                                .idTag(idTag)
                                .reservationId(reservationId)
                                .status(ReservationStatus.ACTIVE)
                                .createdAt(Instant.now())
                                .build();
                        reservationRepo.save(reservation);
                    }
                    return new CommandResult(status, response);
                });
    }
}
