package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.StationConfigurationKey;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.StationConfigurationPort;

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
    public Optional<List<StationConfigurationKey>> getConfiguration(TenantId tenantId,
                                                                     ChargePointIdentity stationIdentity) {
        return Optional.ofNullable(configurations.get(key(tenantId, stationIdentity)));
    }
}
