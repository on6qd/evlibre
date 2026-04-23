package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.diagnostics.DiagnosticsWire;
import com.evlibre.server.core.domain.v201.diagnostics.GetLogStatus;
import com.evlibre.server.core.domain.v201.diagnostics.LogParameters;
import com.evlibre.server.core.domain.v201.diagnostics.LogType;
import com.evlibre.server.core.domain.v201.dto.GetLogResult;
import com.evlibre.server.core.domain.v201.ports.inbound.GetLogPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GetLogUseCaseV201 implements GetLogPort {

    private static final Logger log = LoggerFactory.getLogger(GetLogUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public GetLogUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<GetLogResult> getLog(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            LogType logType,
            int requestId,
            LogParameters logParameters,
            Integer retries,
            Integer retryInterval) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(logType, "logType");
        Objects.requireNonNull(logParameters, "log");
        if (retries != null && retries < 0) {
            throw new IllegalArgumentException("retries must be >= 0 when present, got " + retries);
        }
        if (retryInterval != null && retryInterval < 0) {
            throw new IllegalArgumentException(
                    "retryInterval must be >= 0 when present, got " + retryInterval);
        }

        Map<String, Object> logPayload = new LinkedHashMap<>();
        logPayload.put("remoteLocation", logParameters.remoteLocation());
        if (logParameters.oldestTimestamp() != null) {
            logPayload.put("oldestTimestamp",
                    DateTimeFormatter.ISO_INSTANT.format(logParameters.oldestTimestamp()));
        }
        if (logParameters.latestTimestamp() != null) {
            logPayload.put("latestTimestamp",
                    DateTimeFormatter.ISO_INSTANT.format(logParameters.latestTimestamp()));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("logType", DiagnosticsWire.logTypeToWire(logType));
        payload.put("requestId", requestId);
        if (retries != null) {
            payload.put("retries", retries);
        }
        if (retryInterval != null) {
            payload.put("retryInterval", retryInterval);
        }
        payload.put("log", logPayload);

        log.info("Sending GetLog(requestId={}, logType={}, location={}) to {} (tenant: {})",
                requestId, logType, logParameters.remoteLocation(),
                stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "GetLog", payload)
                .thenApply(response -> parseResponse(stationIdentity, requestId, response));
    }

    private static GetLogResult parseResponse(
            ChargePointIdentity stationIdentity, int requestId, Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        GetLogStatus status = DiagnosticsWire.getLogStatusFromWire(statusWire);
        String filename = response.get("filename") == null
                ? null : String.valueOf(response.get("filename"));
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("GetLog response from {} (requestId={}): {} (filename={})",
                stationIdentity.value(), requestId, statusWire, filename);
        return new GetLogResult(status, filename, statusInfoReason, response);
    }
}
