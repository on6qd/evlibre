package com.evlibre.server.adapter.webui;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.StationEventPublisher;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class EventBusStationEventPublisher implements StationEventPublisher {

    public static final String ADDRESS_PREFIX = "station.updated.";

    private final Vertx vertx;

    public EventBusStationEventPublisher(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void stationUpdated(TenantId tenantId, ChargePointIdentity stationIdentity) {
        JsonObject message = new JsonObject()
                .put("tenantId", tenantId.value())
                .put("stationIdentity", stationIdentity.value());
        vertx.eventBus().publish(ADDRESS_PREFIX + tenantId.value(), message);
    }
}
