package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.UpdateFirmwareResult;
import com.evlibre.server.core.domain.v201.firmware.Firmware;
import com.evlibre.server.core.domain.v201.firmware.UpdateFirmwareStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.UpdateFirmwarePort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class UpdateFirmwareUseCaseV201 implements UpdateFirmwarePort {

    private static final Logger log = LoggerFactory.getLogger(UpdateFirmwareUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public UpdateFirmwareUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<UpdateFirmwareResult> updateFirmware(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int requestId,
            Firmware firmware,
            Integer retries,
            Integer retryInterval) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(firmware, "firmware");
        if (retries != null && retries < 0) {
            throw new IllegalArgumentException("retries must be >= 0 when present, got " + retries);
        }
        if (retryInterval != null && retryInterval < 0) {
            throw new IllegalArgumentException(
                    "retryInterval must be >= 0 when present, got " + retryInterval);
        }

        Map<String, Object> firmwarePayload = new LinkedHashMap<>();
        firmwarePayload.put("location", firmware.location());
        firmwarePayload.put("retrieveDateTime",
                DateTimeFormatter.ISO_INSTANT.format(firmware.retrieveDateTime()));
        if (firmware.installDateTime() != null) {
            firmwarePayload.put("installDateTime",
                    DateTimeFormatter.ISO_INSTANT.format(firmware.installDateTime()));
        }
        if (firmware.signingCertificate() != null) {
            firmwarePayload.put("signingCertificate", firmware.signingCertificate());
        }
        if (firmware.signature() != null) {
            firmwarePayload.put("signature", firmware.signature());
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId);
        if (retries != null) {
            payload.put("retries", retries);
        }
        if (retryInterval != null) {
            payload.put("retryInterval", retryInterval);
        }
        payload.put("firmware", firmwarePayload);

        log.info("Sending UpdateFirmware(requestId={}, location={}) to {} (tenant: {})",
                requestId, firmware.location(), stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "UpdateFirmware", payload)
                .thenApply(response -> parseResponse(stationIdentity, requestId, response));
    }

    private static UpdateFirmwareResult parseResponse(
            ChargePointIdentity stationIdentity, int requestId, Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        UpdateFirmwareStatus status = parseStatus(statusWire);
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("UpdateFirmware response from {} (requestId={}): {}",
                stationIdentity.value(), requestId, statusWire);
        return new UpdateFirmwareResult(status, statusInfoReason, response);
    }

    private static UpdateFirmwareStatus parseStatus(String wire) {
        return switch (wire) {
            case "Accepted" -> UpdateFirmwareStatus.ACCEPTED;
            case "Rejected" -> UpdateFirmwareStatus.REJECTED;
            case "AcceptedCanceled" -> UpdateFirmwareStatus.ACCEPTED_CANCELED;
            case "InvalidCertificate" -> UpdateFirmwareStatus.INVALID_CERTIFICATE;
            case "RevokedCertificate" -> UpdateFirmwareStatus.REVOKED_CERTIFICATE;
            default -> throw new IllegalStateException(
                    "Unexpected UpdateFirmware status from station: " + wire);
        };
    }
}
