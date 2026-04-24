package com.evlibre.server.adapter.webui.handlers;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class StationCommandHandlerV201 {

    private static final Logger log = LoggerFactory.getLogger(StationCommandHandlerV201.class);

    private final Ocpp201StationCommandSender commandSender;
    private final AtomicInteger remoteStartIdSeq = new AtomicInteger(1);
    private final AtomicInteger requestIdSeq = new AtomicInteger(1);

    public StationCommandHandlerV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = commandSender;
    }

    public void clearCache(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        sendCommand(ctx, tenantId, stationId, "ClearCache", Collections.emptyMap());
    }

    public void changeAvailability(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        String status = param(ctx, "operationalStatus");
        if (!"Operative".equals(status) && !"Inoperative".equals(status)) {
            respondResult(ctx, "ChangeAvailability",
                    "error: operationalStatus must be Operative or Inoperative", false);
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("operationalStatus", status);
        String evseStr = param(ctx, "evseId");
        if (evseStr != null && !evseStr.isBlank()) {
            Map<String, Object> evse = new HashMap<>();
            evse.put("id", Integer.parseInt(evseStr));
            String connStr = param(ctx, "connectorId");
            if (connStr != null && !connStr.isBlank()) {
                evse.put("connectorId", Integer.parseInt(connStr));
            }
            payload.put("evse", evse);
        }
        sendCommand(ctx, tenantId, stationId, "ChangeAvailability", payload);
    }

    public void getLog(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        String logType = param(ctx, "logType");
        if (logType == null || logType.isBlank()) logType = "DiagnosticsLog";
        if (!"DiagnosticsLog".equals(logType) && !"SecurityLog".equals(logType)) {
            respondResult(ctx, "GetLog",
                    "error: logType must be DiagnosticsLog or SecurityLog", false);
            return;
        }
        String remoteLocation = param(ctx, "remoteLocation");
        if (remoteLocation == null || remoteLocation.isBlank()) {
            respondResult(ctx, "GetLog",
                    "error: remoteLocation is required", false);
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("logType", logType);
        payload.put("requestId", requestIdSeq.getAndIncrement());
        payload.put("log", Map.of("remoteLocation", remoteLocation));
        sendCommand(ctx, tenantId, stationId, "GetLog", payload);
    }

    public void requestStartTransaction(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        String idTokenValue = param(ctx, "idToken");
        String tokenType = param(ctx, "tokenType");
        if (tokenType == null || tokenType.isBlank()) tokenType = "ISO14443";
        if (idTokenValue == null || idTokenValue.isBlank()) {
            respondResult(ctx, "RequestStartTransaction",
                    "error: idToken is required", false);
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("remoteStartId", remoteStartIdSeq.getAndIncrement());
        payload.put("idToken", Map.of("idToken", idTokenValue, "type", tokenType));
        String evseStr = param(ctx, "evseId");
        if (evseStr != null && !evseStr.isBlank()) {
            payload.put("evseId", Integer.parseInt(evseStr));
        }
        sendCommand(ctx, tenantId, stationId, "RequestStartTransaction", payload);
    }

    public void requestStopTransaction(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        String txId = param(ctx, "transactionId");
        if (txId == null || txId.isBlank()) {
            respondResult(ctx, "RequestStopTransaction",
                    "error: transactionId is required", false);
            return;
        }
        sendCommand(ctx, tenantId, stationId, "RequestStopTransaction",
                Map.of("transactionId", txId));
    }

    public void unlockConnector(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        String evseStr = param(ctx, "evseId");
        String connStr = param(ctx, "connectorId");
        if (evseStr == null || evseStr.isBlank() || connStr == null || connStr.isBlank()) {
            respondResult(ctx, "UnlockConnector",
                    "error: evseId and connectorId are both required", false);
            return;
        }
        Map<String, Object> payload = Map.of(
                "evseId", Integer.parseInt(evseStr),
                "connectorId", Integer.parseInt(connStr));
        sendCommand(ctx, tenantId, stationId, "UnlockConnector", payload);
    }

    public void reset(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        String type = ctx.queryParams().get("type");
        if (type == null || type.isBlank()) type = "Immediate";
        if (!"Immediate".equals(type) && !"OnIdle".equals(type)) {
            respondResult(ctx, "Reset", "error: type must be Immediate or OnIdle", false);
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        String evseStr = ctx.queryParams().get("evseId");
        if (evseStr != null && !evseStr.isBlank()) {
            payload.put("evseId", Integer.parseInt(evseStr));
        }
        sendCommand(ctx, tenantId, stationId, "Reset", payload);
    }

    private void sendCommand(RoutingContext ctx, TenantId tenantId, String stationId,
                              String action, Map<String, Object> payload) {
        ChargePointIdentity identity = new ChargePointIdentity(stationId);

        commandSender.sendCommand(tenantId, identity, action, payload)
                .thenAccept(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    boolean success = "Accepted".equals(status) || "Unlocked".equals(status);
                    ctx.vertx().runOnContext(v -> respondResult(ctx, action, status, success));
                })
                .exceptionally(err -> {
                    log.error("Command {} failed for {}: {}", action, stationId, err.getMessage());
                    ctx.vertx().runOnContext(v -> respondResult(ctx, action, "error: " + err.getMessage(), false));
                    return null;
                });
    }

    private static String param(RoutingContext ctx, String name) {
        String q = ctx.queryParams().get(name);
        if (q != null) return q;
        return ctx.request().getFormAttribute(name);
    }

    private void respondResult(RoutingContext ctx, String action, String status, boolean success) {
        String html = partials.commandResult.template(action, status, success)
                .render()
                .toString();
        ctx.response()
                .putHeader("Content-Type", "text/html; charset=UTF-8")
                .end(html);
    }
}
