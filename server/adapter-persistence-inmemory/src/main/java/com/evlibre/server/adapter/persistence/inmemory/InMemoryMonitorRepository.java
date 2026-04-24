package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedMonitoring;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import com.evlibre.server.core.domain.v201.ports.outbound.MonitorRepositoryPort;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link MonitorRepositoryPort}: upserts per (component, variable)
 * locator so a replayed {@code NotifyMonitoringReport} does not duplicate
 * rows. Mirrors {@link InMemoryDeviceModelRepository}.
 */
public class InMemoryMonitorRepository implements MonitorRepositoryPort {

    private record StationKey(TenantId tenantId, ChargePointIdentity identity) {}
    private record VariableKey(Component component, Variable variable) {}

    private final Map<StationKey, Map<VariableKey, ReportedMonitoring>> store = new ConcurrentHashMap<>();

    @Override
    public void upsert(TenantId tenantId,
                       ChargePointIdentity stationIdentity,
                       List<ReportedMonitoring> reports) {
        var perStation = store.computeIfAbsent(new StationKey(tenantId, stationIdentity),
                k -> new ConcurrentHashMap<>());
        for (ReportedMonitoring r : reports) {
            perStation.put(new VariableKey(r.component(), r.variable()), r);
        }
    }

    @Override
    public List<ReportedMonitoring> findAll(TenantId tenantId, ChargePointIdentity stationIdentity) {
        var perStation = store.get(new StationKey(tenantId, stationIdentity));
        if (perStation == null) {
            return List.of();
        }
        return List.copyOf(perStation.values());
    }
}
