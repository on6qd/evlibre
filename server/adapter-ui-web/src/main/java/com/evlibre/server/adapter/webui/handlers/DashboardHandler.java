package com.evlibre.server.adapter.webui.handlers;

import com.evlibre.server.adapter.webui.dto.DashboardStats;
import com.evlibre.server.core.domain.model.ChargingStation;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.StationRepositoryPort;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DashboardHandler {

    private static final Logger log = LoggerFactory.getLogger(DashboardHandler.class);

    private final Vertx vertx;
    private final StationRepositoryPort stationRepository;

    public DashboardHandler(Vertx vertx, StationRepositoryPort stationRepository) {
        this.vertx = vertx;
        this.stationRepository = stationRepository;
    }

    public void showDashboard(RoutingContext ctx, TenantId tenantId) {
        vertx.<String>executeBlocking(() -> {
            List<ChargingStation> stations = stationRepository.findByTenant(tenantId);
            DashboardStats stats = DashboardStats.calculate(stations);

            return pages.dashboard.template(stats, tenantId.value())
                    .render()
                    .toString();

        }).onSuccess(html -> {
            ctx.response()
                    .putHeader("Content-Type", "text/html; charset=UTF-8")
                    .end(html);

        }).onFailure(err -> {
            log.error("Dashboard rendering failed for tenant: {}", tenantId.value(), err);
            ctx.fail(500, err);
        });
    }
}
