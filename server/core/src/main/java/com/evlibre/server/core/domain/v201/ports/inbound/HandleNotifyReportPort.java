package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedVariable;

import java.util.List;

/**
 * Accepts a single frame of an OCPP 2.0.1 NotifyReport. The implementation is
 * responsible for per-{@code requestId} aggregation and committing the reports
 * when the station marks the last frame with {@code tbc=false}.
 */
public interface HandleNotifyReportPort {

    void handleFrame(TenantId tenantId,
                     ChargePointIdentity stationIdentity,
                     int requestId,
                     int seqNo,
                     boolean tbc,
                     List<ReportedVariable> reports);
}
