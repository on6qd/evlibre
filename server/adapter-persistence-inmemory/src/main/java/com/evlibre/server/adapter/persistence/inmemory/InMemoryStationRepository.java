package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.ChargingStation;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.StationRepositoryPort;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStationRepository implements StationRepositoryPort {

    private final Map<UUID, ChargingStation> store = new ConcurrentHashMap<>();

    @Override
    public void save(ChargingStation station) {
        store.put(station.id(), station);
    }

    @Override
    public Optional<ChargingStation> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<ChargingStation> findByTenantAndIdentity(TenantId tenantId, ChargePointIdentity identity) {
        return store.values().stream()
                .filter(s -> s.tenantId().equals(tenantId) && s.identity().equals(identity))
                .findFirst();
    }

    @Override
    public List<ChargingStation> findByTenant(TenantId tenantId) {
        return store.values().stream()
                .filter(s -> s.tenantId().equals(tenantId))
                .toList();
    }
}
