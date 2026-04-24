package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedMonitoring;

import java.util.List;

/**
 * Accepts a single frame of an OCPP 2.0.1 {@code NotifyMonitoringReport}.
 * The implementation is responsible for per-{@code requestId} aggregation and
 * committing monitors to the repository when the station marks the last
 * frame with {@code tbc=false}.
 *
 * <p>Mirrors {@link HandleNotifyReportPort}'s frame-driven contract; the
 * monitoring variant stores a different domain shape (monitor instances
 * rather than variable attributes) so the ports and repositories are kept
 * separate.
 */
public interface HandleNotifyMonitoringReportPort {

    void handleFrame(TenantId tenantId,
                     ChargePointIdentity stationIdentity,
                     int requestId,
                     int seqNo,
                     boolean tbc,
                     List<ReportedMonitoring> reports);
}
