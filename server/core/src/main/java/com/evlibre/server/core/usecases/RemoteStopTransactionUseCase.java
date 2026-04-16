package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.RegistrationStatus;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.RemoteStopTransactionPort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import com.evlibre.server.core.domain.ports.outbound.StationRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RemoteStopTransactionUseCase implements RemoteStopTransactionPort {

    private static final Logger log = LoggerFactory.getLogger(RemoteStopTransactionUseCase.class);

    private final StationCommandSender commandSender;
    private final StationRepositoryPort stationRepository;

    public RemoteStopTransactionUseCase(StationCommandSender commandSender) {
        this(commandSender, null);
    }

    public RemoteStopTransactionUseCase(StationCommandSender commandSender,
                                         StationRepositoryPort stationRepository) {
        this.commandSender = Objects.requireNonNull(commandSender);
        this.stationRepository = stationRepository;
    }

    @Override
    public CompletableFuture<CommandResult> remoteStop(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                        int transactionId) {
        // OCPP 1.6 §4.2: CSMS SHALL NOT initiate RemoteStop while registration is Pending.
        if (stationRepository != null) {
            var station = stationRepository.findByTenantAndIdentity(tenantId, stationIdentity);
            if (station.isPresent() && station.get().registrationStatus() == RegistrationStatus.PENDING) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "Cannot send RemoteStopTransaction to a station in Pending registration: "
                                + stationIdentity.value()));
            }
        }

        log.info("Sending RemoteStopTransaction to {} (tenant: {}, txId: {})",
                stationIdentity.value(), tenantId.value(), transactionId);

        Map<String, Object> payload = Map.of("transactionId", transactionId);
        return commandSender.sendCommand(tenantId, stationIdentity, "RemoteStopTransaction", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("RemoteStopTransaction response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }
}
