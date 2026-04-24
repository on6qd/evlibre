package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.displaymessage.MessageInfo;
import com.evlibre.server.core.domain.v201.displaymessage.SetDisplayMessageResult;
import com.evlibre.server.core.domain.v201.displaymessage.SetDisplayMessageStatus;
import com.evlibre.server.core.domain.v201.displaymessage.wire.DisplayMessageWire;
import com.evlibre.server.core.domain.v201.ports.inbound.SetDisplayMessagePort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SetDisplayMessageUseCaseV201 implements SetDisplayMessagePort {

    private static final Logger log = LoggerFactory.getLogger(SetDisplayMessageUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public SetDisplayMessageUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<SetDisplayMessageResult> setDisplayMessage(TenantId tenantId,
                                                                          ChargePointIdentity stationIdentity,
                                                                          MessageInfo message) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(message, "message must not be null");

        Map<String, Object> payload = Map.of("message", DisplayMessageWire.messageInfoToWire(message));

        log.info("Sending SetDisplayMessage(id={}, priority={}) to {} (tenant: {})",
                message.id(), message.priority(), stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "SetDisplayMessage", payload)
                .thenApply(SetDisplayMessageUseCaseV201::decodeResponse);
    }

    private static SetDisplayMessageResult decodeResponse(Map<String, Object> response) {
        SetDisplayMessageStatus status = statusFromWire((String) response.get("status"));
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> siMap) {
            Object reason = siMap.get("reasonCode");
            if (reason instanceof String s) {
                statusInfoReason = s;
            }
        }
        return new SetDisplayMessageResult(status, statusInfoReason);
    }

    private static SetDisplayMessageStatus statusFromWire(String wire) {
        return switch (wire) {
            case "Accepted" -> SetDisplayMessageStatus.ACCEPTED;
            case "NotSupportedMessageFormat" -> SetDisplayMessageStatus.NOT_SUPPORTED_MESSAGE_FORMAT;
            case "Rejected" -> SetDisplayMessageStatus.REJECTED;
            case "NotSupportedPriority" -> SetDisplayMessageStatus.NOT_SUPPORTED_PRIORITY;
            case "NotSupportedState" -> SetDisplayMessageStatus.NOT_SUPPORTED_STATE;
            case "UnknownTransaction" -> SetDisplayMessageStatus.UNKNOWN_TRANSACTION;
            default -> throw new IllegalArgumentException(
                    "Unknown SetDisplayMessageStatus wire value: " + wire);
        };
    }
}
