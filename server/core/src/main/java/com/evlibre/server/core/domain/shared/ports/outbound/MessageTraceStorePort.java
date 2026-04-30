package com.evlibre.server.core.domain.shared.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.util.List;

/**
 * Bounded ring buffer of recent OCPP wire frames and connection lifecycle events
 * per (tenant, station). Serves the operator UI's per-station Messages tab for
 * firmware/protocol debugging. Lost on server restart.
 */
public interface MessageTraceStorePort {

    void record(TenantId tenant, ChargePointIdentity station, MessageTraceEntry entry);

    /**
     * Returns entries in insertion order (oldest first). The UI re-orders for display.
     */
    List<MessageTraceEntry> recent(TenantId tenant, ChargePointIdentity station);
}
