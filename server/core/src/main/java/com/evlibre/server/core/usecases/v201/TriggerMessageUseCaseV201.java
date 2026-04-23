package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.dto.TriggerMessageResult;
import com.evlibre.server.core.domain.v201.dto.TriggerMessageStatus;
import com.evlibre.server.core.domain.v201.model.MessageTrigger;
import com.evlibre.server.core.domain.v201.ports.inbound.TriggerMessagePort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class TriggerMessageUseCaseV201 implements TriggerMessagePort {

    private static final Logger log = LoggerFactory.getLogger(TriggerMessageUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public TriggerMessageUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<TriggerMessageResult> triggerMessage(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            MessageTrigger requestedMessage,
            Evse evse) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(requestedMessage, "requestedMessage");
        if (requestedMessage == MessageTrigger.STATUS_NOTIFICATION
                && (evse == null || evse.connectorId() == null)) {
            throw new IllegalArgumentException(
                    "StatusNotification trigger requires evse.connectorId per spec");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestedMessage", triggerToWire(requestedMessage));
        if (evse != null) {
            payload.put("evse", evseToWire(evse));
        }

        log.info("Sending TriggerMessage({}, evse={}) to {} (tenant: {})",
                requestedMessage, evse, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "TriggerMessage", payload)
                .thenApply(response -> parseResponse(stationIdentity, requestedMessage, response));
    }

    private static TriggerMessageResult parseResponse(
            ChargePointIdentity stationIdentity, MessageTrigger requested, Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        TriggerMessageStatus status = parseStatus(statusWire);
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("TriggerMessage({}) response from {}: {}",
                requested, stationIdentity.value(), statusWire);
        return new TriggerMessageResult(status, statusInfoReason, response);
    }

    private static TriggerMessageStatus parseStatus(String wire) {
        return switch (wire) {
            case "Accepted" -> TriggerMessageStatus.ACCEPTED;
            case "Rejected" -> TriggerMessageStatus.REJECTED;
            case "NotImplemented" -> TriggerMessageStatus.NOT_IMPLEMENTED;
            default -> throw new IllegalStateException(
                    "Unexpected TriggerMessage status from station: " + wire);
        };
    }

    private static Map<String, Object> evseToWire(Evse evse) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", evse.id());
        if (evse.connectorId() != null) {
            out.put("connectorId", evse.connectorId());
        }
        return out;
    }

    private static String triggerToWire(MessageTrigger t) {
        return switch (t) {
            case BOOT_NOTIFICATION -> "BootNotification";
            case LOG_STATUS_NOTIFICATION -> "LogStatusNotification";
            case FIRMWARE_STATUS_NOTIFICATION -> "FirmwareStatusNotification";
            case HEARTBEAT -> "Heartbeat";
            case METER_VALUES -> "MeterValues";
            case SIGN_CHARGING_STATION_CERTIFICATE -> "SignChargingStationCertificate";
            case SIGN_V2G_CERTIFICATE -> "SignV2GCertificate";
            case STATUS_NOTIFICATION -> "StatusNotification";
            case TRANSACTION_EVENT -> "TransactionEvent";
            case SIGN_COMBINED_CERTIFICATE -> "SignCombinedCertificate";
            case PUBLISH_FIRMWARE_STATUS_NOTIFICATION -> "PublishFirmwareStatusNotification";
        };
    }
}
