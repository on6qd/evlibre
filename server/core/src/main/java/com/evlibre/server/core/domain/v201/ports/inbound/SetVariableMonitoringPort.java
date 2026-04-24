package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.SetMonitoringData;
import com.evlibre.server.core.domain.v201.devicemodel.SetMonitoringResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code SetVariableMonitoring} (OCPP 2.0.1 N04 — Set
 * Variable Monitoring).
 *
 * <p>Synchronous: the response carries one {@link SetMonitoringResult} per
 * {@link SetMonitoringData} entry. A null {@code id} in the request creates a
 * new monitor; a non-null {@code id} replaces the existing monitor with that
 * id.
 *
 * <p>Per-message caps are exposed by the station through
 * {@code MonitoringCtrlr.ItemsPerMessage.SetVariableMonitoring} and
 * {@code MonitoringCtrlr.BytesPerMessage.SetVariableMonitoring}; the caller is
 * responsible for staying within them.
 */
public interface SetVariableMonitoringPort {

    CompletableFuture<List<SetMonitoringResult>> setVariableMonitoring(TenantId tenantId,
                                                                        ChargePointIdentity stationIdentity,
                                                                        List<SetMonitoringData> monitors);
}
