package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GetTransactionStatusResult;
import com.evlibre.server.core.domain.v201.ports.inbound.GetTransactionStatusPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GetTransactionStatusUseCaseV201 implements GetTransactionStatusPort {

    private static final Logger log = LoggerFactory.getLogger(GetTransactionStatusUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public GetTransactionStatusUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<GetTransactionStatusResult> getTransactionStatus(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            String transactionId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");

        Map<String, Object> payload = new LinkedHashMap<>();
        if (transactionId != null) {
            payload.put("transactionId", transactionId);
        }

        log.info("Sending GetTransactionStatus(transactionId={}) to {} (tenant: {})",
                transactionId, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "GetTransactionStatus", payload)
                .thenApply(response -> parseResponse(stationIdentity, transactionId, response));
    }

    private static GetTransactionStatusResult parseResponse(
            ChargePointIdentity stationIdentity, String transactionId, Map<String, Object> response) {
        Boolean ongoing = boolOrNull(response.get("ongoingIndicator"));
        Object messagesRaw = response.get("messagesInQueue");
        if (!(messagesRaw instanceof Boolean messagesInQueue)) {
            throw new IllegalStateException(
                    "GetTransactionStatus response missing required messagesInQueue: " + response);
        }
        log.info("GetTransactionStatus response from {} for txId={}: ongoing={}, messagesInQueue={}",
                stationIdentity.value(), transactionId, ongoing, messagesInQueue);
        return new GetTransactionStatusResult(ongoing, messagesInQueue, response);
    }

    private static Boolean boolOrNull(Object v) {
        return v instanceof Boolean b ? b : null;
    }
}
