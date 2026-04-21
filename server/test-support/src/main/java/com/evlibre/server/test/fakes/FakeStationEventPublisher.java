package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.StationEventPublisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakeStationEventPublisher implements StationEventPublisher {

    public record PublishedEvent(TenantId tenantId, ChargePointIdentity stationIdentity) {}

    private final List<PublishedEvent> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void stationUpdated(TenantId tenantId, ChargePointIdentity stationIdentity) {
        events.add(new PublishedEvent(tenantId, stationIdentity));
    }

    public List<PublishedEvent> events() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }
}
