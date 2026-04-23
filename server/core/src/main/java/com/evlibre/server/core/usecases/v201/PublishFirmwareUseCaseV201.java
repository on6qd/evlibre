package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GenericStatus;
import com.evlibre.server.core.domain.v201.dto.PublishFirmwareResult;
import com.evlibre.server.core.domain.v201.ports.inbound.PublishFirmwarePort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class PublishFirmwareUseCaseV201 implements PublishFirmwarePort {

    private static final Logger log = LoggerFactory.getLogger(PublishFirmwareUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public PublishFirmwareUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<PublishFirmwareResult> publishFirmware(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int requestId,
            String location,
            String checksum,
            Integer retries,
            Integer retryInterval) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(checksum, "checksum");
        if (location.isBlank()) {
            throw new IllegalArgumentException("location must not be blank");
        }
        if (location.length() > 512) {
            throw new IllegalArgumentException(
                    "location exceeds 512 char limit (" + location.length() + ")");
        }
        if (checksum.length() != 32) {
            // OCA spec lists maxLength=32; MD5 hex is exactly 32 chars and the field
            // exists specifically to carry an MD5 — anything else is misuse.
            throw new IllegalArgumentException(
                    "checksum must be a 32-char MD5 hex string, got length " + checksum.length());
        }
        if (retries != null && retries < 0) {
            throw new IllegalArgumentException("retries must be >= 0 when present, got " + retries);
        }
        if (retryInterval != null && retryInterval < 0) {
            throw new IllegalArgumentException(
                    "retryInterval must be >= 0 when present, got " + retryInterval);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("location", location);
        payload.put("retries", retries);
        payload.put("checksum", checksum);
        payload.put("requestId", requestId);
        payload.put("retryInterval", retryInterval);
        // Strip the optional fields if absent — keep the wire shape minimal.
        if (retries == null) {
            payload.remove("retries");
        }
        if (retryInterval == null) {
            payload.remove("retryInterval");
        }

        log.info("Sending PublishFirmware(requestId={}, location={}) to {} (tenant: {})",
                requestId, location, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "PublishFirmware", payload)
                .thenApply(response -> parseResponse(stationIdentity, requestId, response));
    }

    private static PublishFirmwareResult parseResponse(
            ChargePointIdentity stationIdentity, int requestId, Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        GenericStatus status = switch (statusWire) {
            case "Accepted" -> GenericStatus.ACCEPTED;
            case "Rejected" -> GenericStatus.REJECTED;
            default -> throw new IllegalStateException(
                    "Unexpected PublishFirmware status from station: " + statusWire);
        };
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("PublishFirmware response from {} (requestId={}): {}",
                stationIdentity.value(), requestId, statusWire);
        return new PublishFirmwareResult(status, statusInfoReason, response);
    }
}
