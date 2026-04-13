package com.evlibre.server.adapter.webui.handlers;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.OcppSessionManager;
import com.evlibre.server.adapter.webui.EventBusStationEventPublisher;
import com.evlibre.server.adapter.webui.dto.DashboardStats;
import com.evlibre.server.adapter.webui.dto.StationView;
import com.evlibre.server.core.domain.model.ChargingStation;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.StationRepositoryPort;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class SseHandler {

    private static final Logger log = LoggerFactory.getLogger(SseHandler.class);

    private final Vertx vertx;
    private final StationRepositoryPort stationRepository;
    private final OcppSessionManager sessionManager;

    public SseHandler(Vertx vertx, StationRepositoryPort stationRepository,
                      OcppSessionManager sessionManager) {
        this.vertx = vertx;
        this.stationRepository = stationRepository;
        this.sessionManager = sessionManager;
    }

    public void streamStationUpdates(RoutingContext ctx, TenantId tenantId) {
        HttpServerResponse response = ctx.response();
        response.setChunked(true);
        response.putHeader("Content-Type", "text/event-stream");
        response.putHeader("Cache-Control", "no-cache");
        response.putHeader("Connection", "keep-alive");

        String address = EventBusStationEventPublisher.ADDRESS_PREFIX + tenantId.value();
        MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(address);

        // Debounce: coalesce rapid updates within 500ms
        long[] debounceTimerId = {-1};

        consumer.handler(msg -> {
            if (debounceTimerId[0] != -1) {
                vertx.cancelTimer(debounceTimerId[0]);
            }
            debounceTimerId[0] = vertx.setTimer(500, id -> {
                debounceTimerId[0] = -1;
                renderAndSend(response, tenantId);
            });
        });

        response.closeHandler(v -> {
            log.debug("SSE connection closed for tenant {}", tenantId.value());
            consumer.unregister();
        });
    }

    private void renderAndSend(HttpServerResponse response, TenantId tenantId) {
        vertx.<String>executeBlocking(() -> {
            List<ChargingStation> stations = stationRepository.findByTenant(tenantId);
            Set<ChargePointIdentity> connected = sessionManager.connectedStations(tenantId);

            List<StationView> views = stations.stream()
                    .map(s -> StationView.fromDomain(s, connected))
                    .toList();
            String tableHtml = partials.stationsTableBody.template(views)
                    .render()
                    .toString();

            DashboardStats stats = DashboardStats.calculate(stations, connected);
            String statsHtml = partials.dashboardStats.template(stats)
                    .render()
                    .toString();

            return formatSseEvent("stations-table", tableHtml)
                    + formatSseEvent("dashboard-stats", statsHtml);

        }).onSuccess(sse -> {
            if (!response.closed()) {
                response.write(sse);
            }
        }).onFailure(err -> {
            log.error("Failed to render SSE update for tenant {}", tenantId.value(), err);
        });
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
