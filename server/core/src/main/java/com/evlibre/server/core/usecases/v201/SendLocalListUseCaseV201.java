package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.SendLocalListResult;
import com.evlibre.server.core.domain.v201.dto.SendLocalListStatus;
import com.evlibre.server.core.domain.v201.model.AuthorizationData;
import com.evlibre.server.core.domain.v201.model.UpdateType;
import com.evlibre.server.core.domain.v201.ports.inbound.SendLocalListPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SendLocalListUseCaseV201 implements SendLocalListPort {

    private static final Logger log = LoggerFactory.getLogger(SendLocalListUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public SendLocalListUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<SendLocalListResult> sendLocalList(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int versionNumber,
            UpdateType updateType,
            List<AuthorizationData> localAuthorizationList) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(updateType, "updateType");
        Objects.requireNonNull(localAuthorizationList, "localAuthorizationList");
        if (versionNumber <= 0) {
            throw new IllegalArgumentException(
                    "versionNumber must be > 0 per D01.FR.18, got " + versionNumber);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("versionNumber", versionNumber);
        payload.put("updateType", updateTypeToWire(updateType));
        if (!localAuthorizationList.isEmpty()) {
            List<Map<String, Object>> entries = new ArrayList<>(localAuthorizationList.size());
            for (AuthorizationData data : localAuthorizationList) {
                entries.add(authorizationDataToWire(data));
            }
            payload.put("localAuthorizationList", entries);
        }

        log.info("Sending SendLocalList(version={}, type={}, entries={}) to {} (tenant: {})",
                versionNumber, updateType, localAuthorizationList.size(),
                stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "SendLocalList", payload)
                .thenApply(response -> parseResponse(stationIdentity, response));
    }

    private static SendLocalListResult parseResponse(
            ChargePointIdentity stationIdentity, Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        SendLocalListStatus status = parseStatus(statusWire);
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("SendLocalList response from {}: {}", stationIdentity.value(), statusWire);
        return new SendLocalListResult(status, statusInfoReason, response);
    }

    private static SendLocalListStatus parseStatus(String wire) {
        return switch (wire) {
            case "Accepted" -> SendLocalListStatus.ACCEPTED;
            case "Failed" -> SendLocalListStatus.FAILED;
            case "VersionMismatch" -> SendLocalListStatus.VERSION_MISMATCH;
            default -> throw new IllegalStateException(
                    "Unexpected SendLocalList status from station: " + wire);
        };
    }

    private static String updateTypeToWire(UpdateType t) {
        return switch (t) {
            case DIFFERENTIAL -> "Differential";
            case FULL -> "Full";
        };
    }

    private static Map<String, Object> authorizationDataToWire(AuthorizationData data) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idToken", IdTokenWire.toWire(data.idToken()));
        if (data.idTokenInfo() != null) {
            out.put("idTokenInfo", IdTokenWire.idTokenInfoToWire(data.idTokenInfo()));
        }
        return out;
    }
}
