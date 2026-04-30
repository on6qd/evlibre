package com.evlibre.server.adapter.webui;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.MessageTraceEventPublisher;
import io.vertx.core.Vertx;

public class EventBusMessageTraceEventPublisher implements MessageTraceEventPublisher {

    public static final String ADDRESS_PREFIX = "ocpp.message.";

    private final Vertx vertx;

    public EventBusMessageTraceEventPublisher(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void messageRecorded(TenantId tenant, ChargePointIdentity station, MessageTraceEntry entry) {
        vertx.eventBus().publish(addressFor(tenant, station), MessageTraceEntryCodec.encode(entry));
    }

    public static String addressFor(TenantId tenant, ChargePointIdentity station) {
        return ADDRESS_PREFIX + tenant.value() + "." + station.value();
    }
}
