package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.v16.model.StationConfigurationKey;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.outbound.StationConfigurationPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStationConfigurationRepository implements StationConfigurationPort {

    private final Map<String, List<StationConfigurationKey>> configurations = new ConcurrentHashMap<>();

    private String key(TenantId tenantId, ChargePointIdentity stationIdentity) {
        return tenantId.value() + ":" + stationIdentity.value();
    }

    @Override
    public void saveConfiguration(TenantId tenantId, ChargePointIdentity stationIdentity,
                                  List<StationConfigurationKey> keys) {
        configurations.put(key(tenantId, stationIdentity), List.copyOf(keys));
    }

    @Override
    public synchronized void updateConfigurationKey(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                     StationConfigurationKey updated) {
        // OCPP 1.6: configuration keys are matched case-insensitively. Preserve the
        // original casing of whatever is already stored; only the value changes.
        String storeKey = key(tenantId, stationIdentity);
        List<StationConfigurationKey> existing = configurations.getOrDefault(storeKey, List.of());
        List<StationConfigurationKey> merged = new ArrayList<>(existing.size() + 1);
        boolean replaced = false;
        for (StationConfigurationKey k : existing) {
            if (k.key().equalsIgnoreCase(updated.key())) {
                merged.add(new StationConfigurationKey(k.key(), updated.value(), k.readonly()));
                replaced = true;
            } else {
                merged.add(k);
            }
        }
        if (!replaced) {
            merged.add(updated);
        }
        configurations.put(storeKey, List.copyOf(merged));
    }

    @Override
    public Optional<List<StationConfigurationKey>> getConfiguration(TenantId tenantId,
                                                                     ChargePointIdentity stationIdentity) {
        return Optional.ofNullable(configurations.get(key(tenantId, stationIdentity)));
    }
}
