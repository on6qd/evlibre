package com.evlibre.server.core.domain.v201.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;

/**
 * Fired once per completed {@code NotifyMonitoringReport} transmission — the
 * frame with {@code tbc=false} marks the end of the sequence keyed on
 * {@code requestId}.
 *
 * <p>Kept separate from {@link NotifyReportCompletionPort} so operators /
 * subscribers can distinguish "device model refresh completed" from "monitor
 * inventory refresh completed" without a payload discriminator.
 */
public interface NotifyMonitoringReportCompletionPort {

    void onMonitoringReportComplete(TenantId tenantId,
                                     ChargePointIdentity stationIdentity,
                                     int requestId);
}
