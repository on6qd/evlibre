package com.evlibre.server.adapter.webui.handlers;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.outbound.Ocpp16StationCommandSender;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class StationCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(StationCommandHandler.class);

    private final Ocpp16StationCommandSender commandSender;

    public StationCommandHandler(Ocpp16StationCommandSender commandSender) {
        this.commandSender = commandSender;
    }

    public void reset(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        String type = ctx.queryParams().get("type");
        if (type == null) type = "Soft";

        sendCommand(ctx, tenantId, stationId, "Reset", Map.of("type", type));
    }

    public void changeAvailability(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        int connectorId = Integer.parseInt(ctx.queryParams().get("connectorId"));
        String type = ctx.queryParams().get("type");

        sendCommand(ctx, tenantId, stationId, "ChangeAvailability",
                Map.of("connectorId", connectorId, "type", type));
    }

    public void unlockConnector(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        int connectorId = Integer.parseInt(ctx.queryParams().get("connectorId"));

        sendCommand(ctx, tenantId, stationId, "UnlockConnector",
                Map.of("connectorId", connectorId));
    }

    public void clearCache(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        sendCommand(ctx, tenantId, stationId, "ClearCache", Collections.emptyMap());
    }

    public void remoteStart(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        String idTag = ctx.request().getFormAttribute("idTag");
        String connStr = ctx.request().getFormAttribute("connectorId");

        Map<String, Object> payload = new HashMap<>();
        payload.put("idTag", idTag != null ? idTag : "TAG001");
        if (connStr != null && !connStr.isBlank()) {
            payload.put("connectorId", Integer.parseInt(connStr));
        }

        sendCommand(ctx, tenantId, stationId, "RemoteStartTransaction", payload);
    }

    public void remoteStop(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        String txStr = ctx.request().getFormAttribute("transactionId");

        if (txStr == null || txStr.isBlank()) {
            respondResult(ctx, "RemoteStopTransaction", "error: missing transactionId", false);
            return;
        }

        sendCommand(ctx, tenantId, stationId, "RemoteStopTransaction",
                Map.of("transactionId", Integer.parseInt(txStr)));
    }

    public void getDiagnostics(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        String location = ctx.request().getFormAttribute("location");
        if (location == null || location.isBlank()) {
            respondResult(ctx, "GetDiagnostics", "error: missing location", false);
            return;
        }
        sendCommand(ctx, tenantId, stationId, "GetDiagnostics", Map.of("location", location));
    }

    public void updateFirmware(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        String location = ctx.request().getFormAttribute("location");
        if (location == null || location.isBlank()) {
            respondResult(ctx, "UpdateFirmware", "error: missing location", false);
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("location", location);
        payload.put("retrieveDate", java.time.Instant.now().toString());
        sendCommand(ctx, tenantId, stationId, "UpdateFirmware", payload);
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

    private void respondResult(RoutingContext ctx, String action, String status, boolean success) {
        String html = partials.commandResult.template(action, status, success)
                .render()
                .toString();
        ctx.response()
                .putHeader("Content-Type", "text/html; charset=UTF-8")
                .end(html);
    }
}
