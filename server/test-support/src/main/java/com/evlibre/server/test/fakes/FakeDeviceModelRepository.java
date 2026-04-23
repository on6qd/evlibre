package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedVariable;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import com.evlibre.server.core.domain.v201.ports.outbound.DeviceModelRepositoryPort;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FakeDeviceModelRepository implements DeviceModelRepositoryPort {

    private record StationKey(TenantId tenantId, ChargePointIdentity identity) {}
    private record VariableKey(Component component, Variable variable) {}

    private final Map<StationKey, Map<VariableKey, ReportedVariable>> store = new ConcurrentHashMap<>();

    @Override
    public void upsert(TenantId tenantId, ChargePointIdentity stationIdentity, List<ReportedVariable> reports) {
        Map<VariableKey, ReportedVariable> perStation = store.computeIfAbsent(
                new StationKey(tenantId, stationIdentity), k -> new ConcurrentHashMap<>());
        for (ReportedVariable r : reports) {
            perStation.put(new VariableKey(r.component(), r.variable()), r);
        }
    }

    @Override
    public List<ReportedVariable> findAll(TenantId tenantId, ChargePointIdentity stationIdentity) {
        Map<VariableKey, ReportedVariable> perStation = store.get(new StationKey(tenantId, stationIdentity));
        if (perStation == null) {
            return List.of();
        }
        return List.copyOf(perStation.values());
    }

    public void clear() {
        store.clear();
    }
}
