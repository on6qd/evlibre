package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.RequestStartStopStatus;
import com.evlibre.server.core.domain.v201.dto.RequestStopTransactionResult;
import com.evlibre.server.core.domain.v201.ports.inbound.RequestStopTransactionPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RequestStopTransactionUseCaseV201 implements RequestStopTransactionPort {

    private static final Logger log = LoggerFactory.getLogger(RequestStopTransactionUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public RequestStopTransactionUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<RequestStopTransactionResult> requestStopTransaction(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            String transactionId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(transactionId, "transactionId");
        if (transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId must not be blank");
        }

        Map<String, Object> payload = Map.of("transactionId", transactionId);

        log.info("Sending RequestStopTransaction(txId={}) to {} (tenant: {})",
                transactionId, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "RequestStopTransaction", payload)
                .thenApply(response -> parseResponse(stationIdentity, transactionId, response));
    }

    private static RequestStopTransactionResult parseResponse(
            ChargePointIdentity stationIdentity, String transactionId, Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        RequestStartStopStatus status = parseStatus(statusWire);
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("RequestStopTransaction response from {} for txId={}: {}",
                stationIdentity.value(), transactionId, statusWire);
        return new RequestStopTransactionResult(status, statusInfoReason, response);
    }

    private static RequestStartStopStatus parseStatus(String wire) {
        return switch (wire) {
            case "Accepted" -> RequestStartStopStatus.ACCEPTED;
            case "Rejected" -> RequestStartStopStatus.REJECTED;
            default -> throw new IllegalStateException(
                    "Unexpected RequestStopTransaction status from station: " + wire);
        };
    }
}
