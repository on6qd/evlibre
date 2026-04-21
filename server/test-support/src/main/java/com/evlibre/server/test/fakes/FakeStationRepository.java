package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.ChargingStation;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FakeStationRepository implements StationRepositoryPort {

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

    public Collection<ChargingStation> getAll() {
        return store.values();
    }

    public int count() {
        return store.size();
    }

    public void clear() {
        store.clear();
    }
}
