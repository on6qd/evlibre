package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.v16.model.ReservationStatus;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.inbound.CancelReservationPort;
import com.evlibre.server.core.domain.v16.ports.outbound.ReservationRepositoryPort;
import com.evlibre.server.core.domain.v16.ports.outbound.Ocpp16StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class CancelReservationUseCase implements CancelReservationPort {

    private static final Logger log = LoggerFactory.getLogger(CancelReservationUseCase.class);

    private final Ocpp16StationCommandSender commandSender;
    private final ReservationRepositoryPort reservationRepo;

    public CancelReservationUseCase(Ocpp16StationCommandSender commandSender, ReservationRepositoryPort reservationRepo) {
        this.commandSender = Objects.requireNonNull(commandSender);
        this.reservationRepo = Objects.requireNonNull(reservationRepo);
    }

    @Override
    public CompletableFuture<CommandResult> cancelReservation(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                                int reservationId) {
        log.info("Sending CancelReservation to {} (tenant: {}, reservationId: {})",
                stationIdentity.value(), tenantId.value(), reservationId);

        Map<String, Object> payload = Map.of("reservationId", reservationId);

        return commandSender.sendCommand(tenantId, stationIdentity, "CancelReservation", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("CancelReservation response from {}: {}", stationIdentity.value(), status);

                    if ("Accepted".equals(status)) {
                        reservationRepo.findByReservationId(tenantId, reservationId)
                                .ifPresent(r -> reservationRepo.save(r.withStatus(ReservationStatus.CANCELLED)));
                    }
                    return new CommandResult(status, response);
                });
    }
}
