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

public class StationCommandHandlerV201 {

    private static final Logger log = LoggerFactory.getLogger(StationCommandHandlerV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public StationCommandHandlerV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = commandSender;
    }

    public void clearCache(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");
        sendCommand(ctx, tenantId, stationId, "ClearCache", Collections.emptyMap());
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

    private void respondResult(RoutingContext ctx, String action, String status, boolean success) {
        String html = partials.commandResult.template(action, status, success)
                .render()
                .toString();
        ctx.response()
                .putHeader("Content-Type", "text/html; charset=UTF-8")
                .end(html);
    }
}
