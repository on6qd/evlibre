package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ClearMonitoringResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code ClearVariableMonitoring} (OCPP 2.0.1 N06 — Clear
 * Variable Monitoring).
 *
 * <p>Removes a set of monitors by id. The station returns one
 * {@link ClearMonitoringResult} per requested id so the caller can tell which
 * specific ids failed (e.g. unknown or hardwired monitors that cannot be
 * cleared).
 */
public interface ClearVariableMonitoringPort {

    CompletableFuture<List<ClearMonitoringResult>> clearVariableMonitoring(TenantId tenantId,
                                                                             ChargePointIdentity stationIdentity,
                                                                             List<Integer> monitorIds);
}
