package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.ports.outbound.NotifyMonitoringReportCompletionPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakeNotifyMonitoringReportCompletionPublisher implements NotifyMonitoringReportCompletionPort {

    public record CompletionEvent(TenantId tenantId, ChargePointIdentity stationIdentity, int requestId) {}

    private final List<CompletionEvent> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onMonitoringReportComplete(TenantId tenantId, ChargePointIdentity stationIdentity, int requestId) {
        events.add(new CompletionEvent(tenantId, stationIdentity, requestId));
    }

    public List<CompletionEvent> events() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }
}
