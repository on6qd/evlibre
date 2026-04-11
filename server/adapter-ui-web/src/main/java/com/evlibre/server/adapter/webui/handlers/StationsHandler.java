package com.evlibre.server.adapter.webui.handlers;

import com.evlibre.server.adapter.webui.dto.StationView;
import com.evlibre.server.core.domain.model.ChargingStation;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.StationRepositoryPort;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class StationsHandler {

    private static final Logger log = LoggerFactory.getLogger(StationsHandler.class);

    private final Vertx vertx;
    private final StationRepositoryPort stationRepository;

    public StationsHandler(Vertx vertx, StationRepositoryPort stationRepository) {
        this.vertx = vertx;
        this.stationRepository = stationRepository;
    }

    public void listStations(RoutingContext ctx, TenantId tenantId) {
        vertx.<String>executeBlocking(() -> {
            List<ChargingStation> stations = stationRepository.findByTenant(tenantId);
            List<StationView> views = stations.stream()
                    .map(StationView::fromDomain)
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
