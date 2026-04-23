package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.diagnostics.EventData;
import com.evlibre.server.core.usecases.v201.HandleNotifyEventUseCaseV201;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakeNotifyEventSink implements HandleNotifyEventUseCaseV201.Sink {

    public record Frame(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            Instant generatedAt,
            int seqNo,
            boolean tbc,
            List<EventData> eventData) {}

    private final List<Frame> frames = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onNotifyEvent(TenantId tenantId,
                              ChargePointIdentity stationIdentity,
                              Instant generatedAt,
                              int seqNo,
                              boolean tbc,
                              List<EventData> eventData) {
        frames.add(new Frame(tenantId, stationIdentity, generatedAt, seqNo, tbc, eventData));
    }

    public List<Frame> frames() {
        return List.copyOf(frames);
    }

    public void clear() {
        frames.clear();
    }
}
