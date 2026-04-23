package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.firmware.FirmwareStatus;
import com.evlibre.server.core.usecases.v201.HandleFirmwareStatusNotificationUseCaseV201;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakeFirmwareStatusSink implements HandleFirmwareStatusNotificationUseCaseV201.Sink {

    public record Event(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            FirmwareStatus status,
            Integer requestId) {}

    private final List<Event> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onFirmwareStatus(TenantId tenantId,
                                 ChargePointIdentity stationIdentity,
                                 FirmwareStatus status,
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
