package com.evlibre.server.adapter.webui.handlers;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.OcppSessionManager;
import com.evlibre.server.adapter.webui.dto.MessageRowView;
import com.evlibre.server.adapter.webui.dto.StationView;
import com.evlibre.server.core.domain.shared.model.ChargingStation;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.MessageTraceStorePort;
import com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class StationDetailHandler {

    private static final Logger log = LoggerFactory.getLogger(StationDetailHandler.class);

    private final Vertx vertx;
    private final StationRepositoryPort stationRepository;
    private final OcppSessionManager sessionManager;
    private final MessageTraceStorePort traceStore;
    private final ObjectMapper objectMapper;

    public StationDetailHandler(Vertx vertx, StationRepositoryPort stationRepository,
                                 OcppSessionManager sessionManager) {
        this(vertx, stationRepository, sessionManager, null);
    }

    public StationDetailHandler(Vertx vertx, StationRepositoryPort stationRepository,
                                 OcppSessionManager sessionManager,
                                 MessageTraceStorePort traceStore) {
        this.vertx = vertx;
        this.stationRepository = stationRepository;
        this.sessionManager = sessionManager;
        this.traceStore = traceStore;
        this.objectMapper = new ObjectMapper();
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
            List<MessageRowView> messages = recentMessageRows(tenantId, identity);

            if (station.get().protocol() == OcppProtocol.OCPP_201) {
                return pages.stationDetailV201.template(view, tenantId.value(), messages)
                        .render()
                        .toString();
            }
            return pages.stationDetail.template(view, tenantId.value(), messages)
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

    private List<MessageRowView> recentMessageRows(TenantId tenantId, ChargePointIdentity identity) {
        if (traceStore == null) {
            return List.of();
        }
        List<MessageTraceEntry> entries = traceStore.recent(tenantId, identity);
        // Newest first for display.
        java.util.ArrayList<MessageRowView> rows = new java.util.ArrayList<>(entries.size());
        for (int i = entries.size() - 1; i >= 0; i--) {
            rows.add(MessageRowView.from(entries.get(i), objectMapper));
        }
        return rows;
    }
}
