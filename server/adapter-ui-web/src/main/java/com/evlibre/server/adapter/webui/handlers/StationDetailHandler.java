package com.evlibre.server.adapter.webui.handlers;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.OcppSessionManager;
import com.evlibre.server.adapter.webui.dto.StationView;
import com.evlibre.server.core.domain.model.ChargingStation;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.StationRepositoryPort;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

public class StationDetailHandler {

    private static final Logger log = LoggerFactory.getLogger(StationDetailHandler.class);

    private final Vertx vertx;
    private final StationRepositoryPort stationRepository;
    private final OcppSessionManager sessionManager;

    public StationDetailHandler(Vertx vertx, StationRepositoryPort stationRepository,
                                 OcppSessionManager sessionManager) {
        this.vertx = vertx;
        this.stationRepository = stationRepository;
        this.sessionManager = sessionManager;
    }

    public void showStation(RoutingContext ctx, TenantId tenantId) {
        String stationId = ctx.pathParam("stationId");

        vertx.<String>executeBlocking(() -> {
            ChargePointIdentity identity = new ChargePointIdentity(stationId);
            Optional<ChargingStation> station = stationRepository.findByTenantAndIdentity(tenantId, identity);

            if (station.isEmpty()) {
                return null;
            }

            Set<ChargePointIdentity> connected = sessionManager.connectedStations(tenantId);
            StationView view = StationView.fromDomain(station.get(), connected);

            return pages.stationDetail.template(view, tenantId.value())
                    .render()
                    .toString();

        }).onSuccess(html -> {
            if (html == null) {
                ctx.response().setStatusCode(404)
                        .putHeader("Content-Type", "text/html; charset=UTF-8")
                        .end("<div style='color:#ff4444;font-family:monospace;padding:20px;'>"
                                + "station not found: " + stationId + "</div>");
                return;
            }
            ctx.response()
                    .putHeader("Content-Type", "text/html; charset=UTF-8")
                    .end(html);

        }).onFailure(err -> {
            log.error("Station detail rendering failed: {}", stationId, err);
            ctx.fail(500, err);
        });
    }
}
