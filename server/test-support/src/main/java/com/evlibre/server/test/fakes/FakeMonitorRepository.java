package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedMonitoring;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import com.evlibre.server.core.domain.v201.ports.outbound.MonitorRepositoryPort;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FakeMonitorRepository implements MonitorRepositoryPort {

    private record StationKey(TenantId tenantId, ChargePointIdentity identity) {}
    private record VariableKey(Component component, Variable variable) {}

    private final Map<StationKey, Map<VariableKey, ReportedMonitoring>> store = new ConcurrentHashMap<>();

    @Override
    public void upsert(TenantId tenantId, ChargePointIdentity stationIdentity, List<ReportedMonitoring> reports) {
        Map<VariableKey, ReportedMonitoring> perStation = store.computeIfAbsent(
                new StationKey(tenantId, stationIdentity), k -> new ConcurrentHashMap<>());
        for (ReportedMonitoring r : reports) {
            perStation.put(new VariableKey(r.component(), r.variable()), r);
        }
    }

    @Override
    public List<ReportedMonitoring> findAll(TenantId tenantId, ChargePointIdentity stationIdentity) {
        Map<VariableKey, ReportedMonitoring> perStation = store.get(new StationKey(tenantId, stationIdentity));
        if (perStation == null) {
            return List.of();
        }
        return List.copyOf(perStation.values());
    }

    public void clear() {
        store.clear();
    }
}
