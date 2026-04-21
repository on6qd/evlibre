package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.v201.model.DeviceModelVariable;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.ports.outbound.DeviceModelPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDeviceModelRepository implements DeviceModelPort {

    private final Map<String, List<DeviceModelVariable>> deviceModels = new ConcurrentHashMap<>();

    private String key(TenantId tenantId, ChargePointIdentity stationIdentity) {
        return tenantId.value() + ":" + stationIdentity.value();
    }

    @Override
    public void saveVariables(TenantId tenantId, ChargePointIdentity stationIdentity,
                              List<DeviceModelVariable> variables) {
        String k = key(tenantId, stationIdentity);
        deviceModels.merge(k, List.copyOf(variables), (existing, incoming) -> {
            var merged = new ArrayList<>(existing);
            merged.addAll(incoming);
            return List.copyOf(merged);
        });
    }

    @Override
    public Optional<List<DeviceModelVariable>> getVariables(TenantId tenantId,
                                                             ChargePointIdentity stationIdentity) {
        return Optional.ofNullable(deviceModels.get(key(tenantId, stationIdentity)));
    }
}
