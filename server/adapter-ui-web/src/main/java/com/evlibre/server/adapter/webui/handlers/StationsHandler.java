package com.evlibre.server.adapter.webui.handlers;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.webui.dto.StationView;
import com.evlibre.server.core.domain.model.ChargingStation;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.StationRepositoryPort;
import com.evlibre.server.adapter.ocpp.OcppSessionManager;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class StationsHandler {

    private static final Logger log = LoggerFactory.getLogger(StationsHandler.class);

    private final Vertx vertx;
    private final StationRepositoryPort stationRepository;
    private final OcppSessionManager sessionManager;

    public StationsHandler(Vertx vertx, StationRepositoryPort stationRepository,
                           OcppSessionManager sessionManager) {
        this.vertx = vertx;
        this.stationRepository = stationRepository;
        this.sessionManager = sessionManager;
    }

    public void listStations(RoutingContext ctx, TenantId tenantId) {
        vertx.<String>executeBlocking(() -> {
            List<ChargingStation> stations = stationRepository.findByTenant(tenantId);
            Set<ChargePointIdentity> connected = sessionManager.connectedStations(tenantId);
            List<StationView> views = stations.stream()
                    .map(s -> StationView.fromDomain(s, connected))
                    .toList();

            return pages.stations.template(views, tenantId.value())
                    .render()
                    .toString();

        }).onSuccess(html -> {
            ctx.response()
                    .putHeader("Content-Type", "text/html; charset=UTF-8")
                    .end(html);

        }).onFailure(err -> {
            log.error("Stations page rendering failed for tenant: {}", tenantId.value(), err);
            ctx.fail(500, err);
        });
    }
}
