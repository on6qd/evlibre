package com.evlibre.server.adapter.webui.handlers;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.webui.EventBusMessageTraceEventPublisher;
import com.evlibre.server.adapter.webui.MessageTraceEntryCodec;
import com.evlibre.server.adapter.webui.dto.MessageRowView;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.MessageTraceStorePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagesHandler {

    private static final Logger log = LoggerFactory.getLogger(MessagesHandler.class);

    private final Vertx vertx;
    private final MessageTraceStorePort traceStore;
    private final ObjectMapper objectMapper;

    public MessagesHandler(Vertx vertx, MessageTraceStorePort traceStore, ObjectMapper objectMapper) {
        this.vertx = vertx;
        this.traceStore = traceStore;
        this.objectMapper = objectMapper;
    }

    /**
     * Server-Sent Events stream of newly recorded trace entries for one (tenant, station).
     * Emits one {@code event: message-row} per entry, payload = pre-rendered Rocker HTML
     * for the row. Subscribers htmx-prepend each event into the messages list.
     */
    public void streamMessages(RoutingContext ctx, TenantId tenantId) {
        String stationIdStr = ctx.pathParam("stationId");
        ChargePointIdentity station;
        try {
            station = new ChargePointIdentity(stationIdStr);
        } catch (Exception e) {
            ctx.response().setStatusCode(400).end("Invalid stationId");
            return;
        }

        HttpServerResponse response = ctx.response();
        response.setChunked(true);
        response.putHeader("Content-Type", "text/event-stream");
        response.putHeader("Cache-Control", "no-cache");
        response.putHeader("Connection", "keep-alive");

        String address = EventBusMessageTraceEventPublisher.addressFor(tenantId, station);
        MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(address);

        consumer.handler(msg -> renderAndSend(response, msg.body()));

        response.closeHandler(v -> {
            log.debug("Messages SSE closed for tenant={} station={}", tenantId.value(), station.value());
            consumer.unregister();
        });
    }

    private void renderAndSend(HttpServerResponse response, JsonObject json) {
        vertx.<String>executeBlocking(() -> {
            MessageTraceEntry entry = MessageTraceEntryCodec.decode(json);
            MessageRowView row = MessageRowView.from(entry, objectMapper);
            String html = partials._messageRow.template(row).render().toString();
            return formatSseEvent("message-row", html);
        }).onSuccess(sse -> {
            if (!response.closed()) {
                response.write(sse);
            }
        }).onFailure(err -> log.error("Failed to render message-row SSE event", err));
    }

    private String formatSseEvent(String eventName, String html) {
        StringBuilder sb = new StringBuilder();
        sb.append("event: ").append(eventName).append('\n');
        for (String line : html.split("\n")) {
            sb.append("data: ").append(line).append('\n');
        }
        sb.append('\n');
        return sb.toString();
    }
}
