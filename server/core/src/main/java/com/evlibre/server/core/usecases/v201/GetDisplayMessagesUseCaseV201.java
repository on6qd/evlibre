package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.displaymessage.GetDisplayMessagesResult;
import com.evlibre.server.core.domain.v201.displaymessage.GetDisplayMessagesStatus;
import com.evlibre.server.core.domain.v201.displaymessage.MessagePriority;
import com.evlibre.server.core.domain.v201.displaymessage.MessageState;
import com.evlibre.server.core.domain.v201.displaymessage.wire.DisplayMessageWire;
import com.evlibre.server.core.domain.v201.ports.inbound.GetDisplayMessagesPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GetDisplayMessagesUseCaseV201 implements GetDisplayMessagesPort {

    private static final Logger log = LoggerFactory.getLogger(GetDisplayMessagesUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public GetDisplayMessagesUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<GetDisplayMessagesResult> getDisplayMessages(TenantId tenantId,
                                                                            ChargePointIdentity stationIdentity,
                                                                            int requestId,
                                                                            List<Integer> ids,
                                                                            MessagePriority priority,
                                                                            MessageState state) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(ids, "ids must not be null (use empty list for 'any')");
        for (Integer id : ids) {
            if (id == null) {
                throw new IllegalArgumentException("GetDisplayMessages ids must not contain null");
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId);
        if (!ids.isEmpty()) {
            payload.put("id", List.copyOf(ids));
        }
        if (priority != null) {
            payload.put("priority", DisplayMessageWire.priorityToWire(priority));
        }
        if (state != null) {
            payload.put("state", DisplayMessageWire.stateToWire(state));
        }

        log.info("Sending GetDisplayMessages(requestId={}, ids={}, priority={}, state={}) to {} (tenant: {})",
                requestId, ids, priority, state, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "GetDisplayMessages", payload)
                .thenApply(GetDisplayMessagesUseCaseV201::decodeResponse);
    }

    private static GetDisplayMessagesResult decodeResponse(Map<String, Object> response) {
        GetDisplayMessagesStatus status = statusFromWire((String) response.get("status"));
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> siMap) {
            Object reason = siMap.get("reasonCode");
            if (reason instanceof String s) {
                statusInfoReason = s;
            }
        }
        return new GetDisplayMessagesResult(status, statusInfoReason);
    }

    private static GetDisplayMessagesStatus statusFromWire(String wire) {
        return switch (wire) {
            case "Accepted" -> GetDisplayMessagesStatus.ACCEPTED;
            case "Unknown" -> GetDisplayMessagesStatus.UNKNOWN;
            default -> throw new IllegalArgumentException(
                    "Unknown GetDisplayMessagesStatus wire value: " + wire);
        };
    }
}
