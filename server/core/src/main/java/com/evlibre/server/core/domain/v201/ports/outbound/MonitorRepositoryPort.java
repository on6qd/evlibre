package com.evlibre.server.core.domain.v201.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedMonitoring;

import java.util.List;

/**
 * Outbound port for caching the current {@code VariableMonitor} configuration
 * a station has reported via {@code NotifyMonitoringReport}.
 *
 * <p>{@code upsert} replaces the full per-(component, variable) monitor list
 * atomically — the station reports its complete monitor set per locator, so
 * there is no merge semantics to worry about within a single locator.
 */
public interface MonitorRepositoryPort {

    void upsert(TenantId tenantId,
                ChargePointIdentity stationIdentity,
                List<ReportedMonitoring> reports);

    List<ReportedMonitoring> findAll(TenantId tenantId, ChargePointIdentity stationIdentity);
}
