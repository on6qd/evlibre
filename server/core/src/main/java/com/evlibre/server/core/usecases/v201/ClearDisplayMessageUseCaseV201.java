package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.displaymessage.ClearDisplayMessageResult;
import com.evlibre.server.core.domain.v201.displaymessage.ClearMessageStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.ClearDisplayMessagePort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ClearDisplayMessageUseCaseV201 implements ClearDisplayMessagePort {

    private static final Logger log = LoggerFactory.getLogger(ClearDisplayMessageUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public ClearDisplayMessageUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<ClearDisplayMessageResult> clearDisplayMessage(TenantId tenantId,
                                                                              ChargePointIdentity stationIdentity,
                                                                              int id) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);

        Map<String, Object> payload = Map.of("id", id);

        log.info("Sending ClearDisplayMessage(id={}) to {} (tenant: {})",
                id, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "ClearDisplayMessage", payload)
                .thenApply(ClearDisplayMessageUseCaseV201::decodeResponse);
    }

    private static ClearDisplayMessageResult decodeResponse(Map<String, Object> response) {
        ClearMessageStatus status = statusFromWire((String) response.get("status"));
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> siMap) {
            Object reason = siMap.get("reasonCode");
            if (reason instanceof String s) {
                statusInfoReason = s;
            }
        }
        return new ClearDisplayMessageResult(status, statusInfoReason);
    }

    private static ClearMessageStatus statusFromWire(String wire) {
        return switch (wire) {
            case "Accepted" -> ClearMessageStatus.ACCEPTED;
            case "Unknown" -> ClearMessageStatus.UNKNOWN;
            default -> throw new IllegalArgumentException(
                    "Unknown ClearMessageStatus wire value: " + wire);
        };
    }
}
