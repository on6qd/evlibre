package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.RequestStartStopStatus;
import com.evlibre.server.core.domain.v201.dto.RequestStartTransactionResult;
import com.evlibre.server.core.domain.v201.model.AdditionalInfo;
import com.evlibre.server.core.domain.v201.model.IdToken;
import com.evlibre.server.core.domain.v201.model.IdTokenType;
import com.evlibre.server.core.domain.v201.ports.inbound.RequestStartTransactionPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RequestStartTransactionUseCaseV201 implements RequestStartTransactionPort {

    private static final Logger log = LoggerFactory.getLogger(RequestStartTransactionUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public RequestStartTransactionUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<RequestStartTransactionResult> requestStartTransaction(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int remoteStartId,
            IdToken idToken,
            Integer evseId,
            IdToken groupIdToken) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(idToken, "idToken");
        if (evseId != null && evseId <= 0) {
            throw new IllegalArgumentException("evseId must be > 0 when present, got " + evseId);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("remoteStartId", remoteStartId);
        payload.put("idToken", idTokenToWire(idToken));
        if (evseId != null) {
            payload.put("evseId", evseId);
        }
        if (groupIdToken != null) {
            payload.put("groupIdToken", idTokenToWire(groupIdToken));
        }

        log.info("Sending RequestStartTransaction(remoteStartId={}, evseId={}) to {} (tenant: {})",
                remoteStartId, evseId, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "RequestStartTransaction", payload)
                .thenApply(response -> parseResponse(stationIdentity, response));
    }

    private static RequestStartTransactionResult parseResponse(
            ChargePointIdentity stationIdentity, Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        RequestStartStopStatus status = parseStatus(statusWire);
        String transactionId = stringOrNull(response.get("transactionId"));
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            statusInfoReason = stringOrNull(m.get("reasonCode"));
        }
        log.info("RequestStartTransaction response from {}: {} (txId={})",
                stationIdentity.value(), statusWire, transactionId);
        return new RequestStartTransactionResult(status, transactionId, statusInfoReason, response);
    }

    private static RequestStartStopStatus parseStatus(String wire) {
        return switch (wire) {
            case "Accepted" -> RequestStartStopStatus.ACCEPTED;
            case "Rejected" -> RequestStartStopStatus.REJECTED;
            default -> throw new IllegalStateException(
                    "Unexpected RequestStartTransaction status from station: " + wire);
        };
    }

    private static String stringOrNull(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Map<String, Object> idTokenToWire(IdToken token) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idToken", token.idToken());
        out.put("type", idTokenTypeToWire(token.type()));
        if (token.additionalInfo() != null) {
            List<Map<String, Object>> extras = new ArrayList<>(token.additionalInfo().size());
            for (AdditionalInfo info : token.additionalInfo()) {
                extras.add(Map.of(
                        "additionalIdToken", info.additionalIdToken(),
                        "type", info.type()));
            }
            out.put("additionalInfo", extras);
        }
        return out;
    }

    private static String idTokenTypeToWire(IdTokenType t) {
        return switch (t) {
            case CENTRAL -> "Central";
            case EMAID -> "eMAID";
            case ISO14443 -> "ISO14443";
            case ISO15693 -> "ISO15693";
            case KEY_CODE -> "KeyCode";
            case LOCAL -> "Local";
            case MAC_ADDRESS -> "MacAddress";
            case NO_AUTHORIZATION -> "NoAuthorization";
        };
    }
}
