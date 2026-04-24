package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.ports.outbound.CustomerInformationSink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakeCustomerInformationSink implements CustomerInformationSink {

    public record Event(TenantId tenantId, ChargePointIdentity stationIdentity, int requestId, String data) {}

    private final List<Event> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onCustomerInformation(TenantId tenantId, ChargePointIdentity stationIdentity,
                                       int requestId, String data) {
        events.add(new Event(tenantId, stationIdentity, requestId, data));
    }

    public List<Event> events() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }
}
