package com.evlibre.server.adapter.webui.handlers;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.webui.dto.DashboardStats;
import com.evlibre.server.core.domain.shared.model.ChargingStation;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort;
import com.evlibre.server.adapter.ocpp.OcppSessionManager;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class DashboardHandler {

    private static final Logger log = LoggerFactory.getLogger(DashboardHandler.class);

    private final Vertx vertx;
    private final StationRepositoryPort stationRepository;
    private final OcppSessionManager sessionManager;

    public DashboardHandler(Vertx vertx, StationRepositoryPort stationRepository,
                            OcppSessionManager sessionManager) {
        this.vertx = vertx;
        this.stationRepository = stationRepository;
        this.sessionManager = sessionManager;
    }

    public void showDashboard(RoutingContext ctx, TenantId tenantId) {
        vertx.<String>executeBlocking(() -> {
            List<ChargingStation> stations = stationRepository.findByTenant(tenantId);
            Set<ChargePointIdentity> connected = sessionManager.connectedStations(tenantId);
            DashboardStats stats = DashboardStats.calculate(stations, connected);

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
