package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.diagnostics.EventData;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyEventPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class HandleNotifyEventUseCaseV201 implements HandleNotifyEventPort {

    @FunctionalInterface
    public interface Sink {
        void onNotifyEvent(TenantId tenantId,
                           ChargePointIdentity stationIdentity,
                           Instant generatedAt,
                           int seqNo,
                           boolean tbc,
                           List<EventData> eventData);
    }

    private static final Logger log = LoggerFactory.getLogger(HandleNotifyEventUseCaseV201.class);

    private final Sink sink;

    public HandleNotifyEventUseCaseV201(Sink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    @Override
    public void handleNotifyEvent(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            Instant generatedAt,
            int seqNo,
            boolean tbc,
            List<EventData> eventData) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(generatedAt, "generatedAt");
        Objects.requireNonNull(eventData, "eventData");
        if (eventData.isEmpty()) {
            throw new IllegalArgumentException("eventData must contain at least one entry");
        }
        if (seqNo < 0) {
            throw new IllegalArgumentException("seqNo must be >= 0, got " + seqNo);
        }
        log.info("NotifyEvent from {} (seqNo={}, tbc={}, events={})",
                stationIdentity.value(), seqNo, tbc, eventData.size());
        sink.onNotifyEvent(tenantId, stationIdentity, generatedAt, seqNo, tbc, List.copyOf(eventData));
    }
}
