package com.evlibre.server.core.domain.v201.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;

/**
 * Fires when a multi-frame NotifyReport sequence has been fully received
 * (the frame with {@code tbc=false} has arrived and its reports are persisted).
 * Callers can subscribe to react to a completed GetBaseReport/GetReport run.
 */
public interface NotifyReportCompletionPort {

    void onReportComplete(TenantId tenantId,
                          ChargePointIdentity stationIdentity,
                          int requestId);
}
