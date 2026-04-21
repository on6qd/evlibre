package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.RegistrationStatus;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.inbound.RemoteStartTransactionPort;
import com.evlibre.server.core.domain.v16.ports.outbound.Ocpp16StationCommandSender;
import com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RemoteStartTransactionUseCase implements RemoteStartTransactionPort {

    private static final Logger log = LoggerFactory.getLogger(RemoteStartTransactionUseCase.class);

    private final Ocpp16StationCommandSender commandSender;
    private final StationRepositoryPort stationRepository;

    public RemoteStartTransactionUseCase(Ocpp16StationCommandSender commandSender) {
        this(commandSender, null);
    }

    public RemoteStartTransactionUseCase(Ocpp16StationCommandSender commandSender,
                                          StationRepositoryPort stationRepository) {
        this.commandSender = Objects.requireNonNull(commandSender);
        this.stationRepository = stationRepository;
    }

    @Override
    public CompletableFuture<CommandResult> remoteStart(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                         String idTag, Integer connectorId) {
        return remoteStart(tenantId, stationIdentity, idTag, connectorId, null);
    }

    @Override
    public CompletableFuture<CommandResult> remoteStart(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                         String idTag, Integer connectorId,
                                                         Map<String, Object> chargingProfile) {
        // OCPP 1.6 §4.2: CSMS SHALL NOT initiate RemoteStart/RemoteStop while the station's
        // registration is Pending — it is not yet fully onboarded.
        if (stationRepository != null) {
            var station = stationRepository.findByTenantAndIdentity(tenantId, stationIdentity);
            if (station.isPresent() && station.get().registrationStatus() == RegistrationStatus.PENDING) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "Cannot send RemoteStartTransaction to a station in Pending registration: "
                                + stationIdentity.value()));
            }
        }

        log.info("Sending RemoteStartTransaction to {} (tenant: {}, idTag: {})",
                stationIdentity.value(), tenantId.value(), idTag);

        Map<String, Object> payload = new HashMap<>();
        payload.put("idTag", idTag);
        if (connectorId != null) {
            payload.put("connectorId", connectorId);
        }
        if (chargingProfile != null) {
            // OCPP 1.6 §5.15: a chargingProfile included in RemoteStartTransaction MUST have
            // chargingProfilePurpose=TxProfile. Reject early so the CP can't respond Rejected.
            Object purpose = chargingProfile.get("chargingProfilePurpose");
            if (!"TxProfile".equals(purpose)) {
                return CompletableFuture.failedFuture(new IllegalArgumentException(
                        "RemoteStartTransaction chargingProfile.chargingProfilePurpose must be TxProfile, was: "
                                + purpose));
            }
            payload.put("chargingProfile", chargingProfile);
        }

        return commandSender.sendCommand(tenantId, stationIdentity, "RemoteStartTransaction", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("RemoteStartTransaction response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }
}
