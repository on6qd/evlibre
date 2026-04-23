package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.diagnostics.EventData;

import java.time.Instant;
import java.util.List;

/**
 * Inbound port for {@code NotifyEventRequest} (block N07/N08). The station
 * reports one or more events in a batch; large batches arrive as multiple
 * {@code NotifyEvent} messages keyed by an ascending {@code seqNo}, with
 * {@code tbc=true} on every part except the last.
 */
public interface HandleNotifyEventPort {

    void handleNotifyEvent(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            Instant generatedAt,
            int seqNo,
            boolean tbc,
            List<EventData> eventData);
}
