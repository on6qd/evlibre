package com.evlibre.server.core.domain.shared.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry;
import com.evlibre.server.core.domain.shared.model.TenantId;

/**
 * Live notification of newly-recorded message trace entries. The operator UI
 * subscribes per-station to drive the live tail in the Messages tab. Counterpart
 * to {@link MessageTraceStorePort}, which holds the bounded scrollback.
 */
public interface MessageTraceEventPublisher {

    void messageRecorded(TenantId tenant, ChargePointIdentity station, MessageTraceEntry entry);
}
