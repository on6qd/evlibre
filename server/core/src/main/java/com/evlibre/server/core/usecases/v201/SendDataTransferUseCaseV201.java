package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.DataTransferResult;
import com.evlibre.server.core.domain.v201.dto.DataTransferStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.SendDataTransferPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SendDataTransferUseCaseV201 implements SendDataTransferPort {

    private static final Logger log = LoggerFactory.getLogger(SendDataTransferUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public SendDataTransferUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<DataTransferResult> sendDataTransfer(TenantId tenantId,
                                                                    ChargePointIdentity stationIdentity,
                                                                    String vendorId,
                                                                    String messageId,
                                                                    Object data) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(vendorId, "vendorId");

        log.info("Sending DataTransfer to {} (tenant: {}, vendor: {}, messageId: {})",
                stationIdentity.value(), tenantId.value(), vendorId, messageId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("vendorId", vendorId);
        if (messageId != null) {
            payload.put("messageId", messageId);
        }
        if (data != null) {
            // `data` is anyType — pass whatever the caller supplied (primitive, list, or map)
            // straight through; the codec serializes it as JSON.
            payload.put("data", data);
        }

        return commandSender.sendCommand(tenantId, stationIdentity, "DataTransfer", payload)
                .thenApply(response -> parseResponse(stationIdentity, response));
    }

    private static DataTransferResult parseResponse(ChargePointIdentity stationIdentity,
                                                     Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        DataTransferStatus status = parseStatus(statusWire);
        Object responseData = response.get("data");
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("DataTransfer response from {}: {}", stationIdentity.value(), statusWire);
        return new DataTransferResult(status, responseData, statusInfoReason, response);
    }

    private static DataTransferStatus parseStatus(String wire) {
        return switch (wire) {
            case "Accepted" -> DataTransferStatus.ACCEPTED;
            case "Rejected" -> DataTransferStatus.REJECTED;
            case "UnknownMessageId" -> DataTransferStatus.UNKNOWN_MESSAGE_ID;
            case "UnknownVendorId" -> DataTransferStatus.UNKNOWN_VENDOR_ID;
            default -> throw new IllegalStateException(
                    "Unexpected DataTransfer status from station: " + wire);
        };
    }
}
