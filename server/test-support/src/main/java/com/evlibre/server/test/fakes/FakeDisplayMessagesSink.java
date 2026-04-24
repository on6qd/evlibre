package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.displaymessage.MessageInfo;
import com.evlibre.server.core.domain.v201.ports.outbound.DisplayMessagesSink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakeDisplayMessagesSink implements DisplayMessagesSink {

    public record Event(TenantId tenantId, ChargePointIdentity stationIdentity,
                         int requestId, List<MessageInfo> messages) {}

    private final List<Event> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onDisplayMessages(TenantId tenantId, ChargePointIdentity stationIdentity,
                                    int requestId, List<MessageInfo> messages) {
        events.add(new Event(tenantId, stationIdentity, requestId, messages));
    }

    public List<Event> events() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }
}
