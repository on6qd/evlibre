package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ComponentVariableSelector;
import com.evlibre.server.core.domain.v201.devicemodel.MonitoringCriterion;
import com.evlibre.server.core.domain.v201.dto.GetMonitoringReportResult;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code GetMonitoringReport} (OCPP 2.0.1 N03 — Get
 * Monitoring Report).
 *
 * <p>Requests the station stream back its current monitor configuration via
 * one or more {@code NotifyMonitoringReport} calls keyed on {@code requestId}.
 *
 * <p>Both filters are optional: empty {@code selectors} means "no
 * component-variable filter"; empty {@code criteria} means "all monitor
 * kinds". The spec caps {@code criteria} at 3 entries (the full enum), which
 * is enforced at construction.
 */
public interface GetMonitoringReportPort {

    CompletableFuture<GetMonitoringReportResult> getMonitoringReport(TenantId tenantId,
                                                                      ChargePointIdentity stationIdentity,
                                                                      int requestId,
                                                                      Set<MonitoringCriterion> criteria,
                                                                      List<ComponentVariableSelector> selectors);
}
