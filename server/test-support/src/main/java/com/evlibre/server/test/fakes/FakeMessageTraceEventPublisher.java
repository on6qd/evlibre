package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.MessageTraceEventPublisher;

import java.util.ArrayList;
import java.util.List;

public class FakeMessageTraceEventPublisher implements MessageTraceEventPublisher {

    public final List<Published> events = new ArrayList<>();

    @Override
    public synchronized void messageRecorded(TenantId tenant, ChargePointIdentity station, MessageTraceEntry entry) {
        events.add(new Published(tenant, station, entry));
    }

    public record Published(TenantId tenant, ChargePointIdentity station, MessageTraceEntry entry) {}
}
