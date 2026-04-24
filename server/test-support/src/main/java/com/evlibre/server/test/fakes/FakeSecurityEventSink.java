package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.security.SecurityEvent;
import com.evlibre.server.core.usecases.v201.HandleSecurityEventNotificationUseCaseV201;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakeSecurityEventSink implements HandleSecurityEventNotificationUseCaseV201.Sink {

    public record Event(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            SecurityEvent event) {}

    private final List<Event> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onSecurityEvent(TenantId tenantId,
                                ChargePointIdentity stationIdentity,
                                SecurityEvent event) {
        events.add(new Event(tenantId, stationIdentity, event));
    }

    public List<Event> events() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }
}
