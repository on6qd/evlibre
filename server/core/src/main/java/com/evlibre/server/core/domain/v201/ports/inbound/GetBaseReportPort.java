package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ReportBase;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code GetBaseReport} (OCPP 2.0.1 B07). The caller supplies a
 * {@code requestId}; the station echoes it on every follow-up {@code
 * NotifyReport} frame (spec req. B07.FR.04).
 *
 * <p><b>Correlation is not yet wired on the inbound side.</b>
 * {@link com.evlibre.server.adapter.ocpp.handler.v201.NotifyReportHandler201
 * NotifyReportHandler201} today upserts every frame's {@code reportData}
 * directly into the repository — it does not aggregate by {@code requestId}
 * or wait for {@code tbc=false} to commit. Implementing per-request
 * aggregation is a follow-up task; passing a unique {@code requestId} here is
 * still worthwhile to tag log entries and to prepare for the future wiring.
 */
public interface GetBaseReportPort {

    CompletableFuture<CommandResult> getBaseReport(TenantId tenantId,
                                                    ChargePointIdentity stationIdentity,
                                                    int requestId,
                                                    ReportBase reportBase);
}
