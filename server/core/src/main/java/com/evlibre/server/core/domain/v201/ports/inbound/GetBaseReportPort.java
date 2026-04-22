package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ReportBase;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code GetBaseReport} (OCPP 2.0.1 B07). The caller supplies a
 * {@code requestId}; the station echoes it on every follow-up {@code NotifyReport}
 * frame (spec req. B07.FR.04) so responses can be correlated.
 */
public interface GetBaseReportPort {

    CompletableFuture<CommandResult> getBaseReport(TenantId tenantId,
                                                    ChargePointIdentity stationIdentity,
                                                    int requestId,
                                                    ReportBase reportBase);
}
