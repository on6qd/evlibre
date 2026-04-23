package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.diagnostics.UploadLogStatus;
import com.evlibre.server.core.usecases.v201.HandleLogStatusNotificationUseCaseV201;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakeLogStatusSink implements HandleLogStatusNotificationUseCaseV201.Sink {

    public record Event(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            UploadLogStatus status,
            Integer requestId) {}

    private final List<Event> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onLogStatus(TenantId tenantId,
                            ChargePointIdentity stationIdentity,
                            UploadLogStatus status,
                            Integer requestId) {
        events.add(new Event(tenantId, stationIdentity, status, requestId));
    }

    public List<Event> events() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }
}
