package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.ports.inbound.CostUpdatedPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class CostUpdatedUseCaseV201 implements CostUpdatedPort {

    private static final Logger log = LoggerFactory.getLogger(CostUpdatedUseCaseV201.class);

    private static final int TRANSACTION_ID_MAX = 36;

    private final Ocpp201StationCommandSender commandSender;

    public CostUpdatedUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<Void> costUpdated(TenantId tenantId,
                                                 ChargePointIdentity stationIdentity,
                                                 double totalCost,
                                                 String transactionId) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        if (transactionId.length() > TRANSACTION_ID_MAX) {
            throw new IllegalArgumentException(
                    "CostUpdated.transactionId must be <= " + TRANSACTION_ID_MAX
                            + " chars, got " + transactionId.length());
        }

        Map<String, Object> payload = Map.of(
                "totalCost", totalCost,
                "transactionId", transactionId);

        log.info("Sending CostUpdated(totalCost={}, transactionId={}) to {} (tenant: {})",
                totalCost, transactionId, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "CostUpdated", payload)
                .thenApply(response -> null);
    }
}
