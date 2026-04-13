package com.evlibre.server.test.fakes;

import com.evlibre.server.core.domain.model.Tenant;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.TenantRepositoryPort;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class FakeTenantRepository implements TenantRepositoryPort {

    private final Map<String, Tenant> store = new ConcurrentHashMap<>();

    @Override
    public void save(Tenant tenant) {
        store.put(tenant.tenantId().value(), tenant);
    }

    @Override
    public Optional<Tenant> findByTenantId(TenantId tenantId) {
        return Optional.ofNullable(store.get(tenantId.value()));
    }

    public Collection<Tenant> getAll() {
        return store.values();
    }

    public void clear() {
        store.clear();
    }
}
