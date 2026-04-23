package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.firmware.PublishFirmwareStatus;
import com.evlibre.server.core.usecases.v201.HandlePublishFirmwareStatusNotificationUseCaseV201;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakePublishFirmwareStatusSink
        implements HandlePublishFirmwareStatusNotificationUseCaseV201.Sink {

    public record Event(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            PublishFirmwareStatus status,
            List<String> locations,
            Integer requestId) {}

    private final List<Event> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onPublishStatus(TenantId tenantId,
                                ChargePointIdentity stationIdentity,
                                PublishFirmwareStatus status,
                                List<String> locations,
                                Integer requestId) {
        events.add(new Event(tenantId, stationIdentity, status, locations, requestId));
    }

    public List<Event> events() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }
}
