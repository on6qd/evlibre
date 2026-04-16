package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.RemoteStartTransactionPort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RemoteStartTransactionUseCase implements RemoteStartTransactionPort {

    private static final Logger log = LoggerFactory.getLogger(RemoteStartTransactionUseCase.class);

    private final StationCommandSender commandSender;

    public RemoteStartTransactionUseCase(StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
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
