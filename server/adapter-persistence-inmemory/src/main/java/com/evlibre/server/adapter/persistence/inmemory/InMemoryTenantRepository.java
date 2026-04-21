package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.server.core.domain.shared.model.Tenant;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.TenantRepositoryPort;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTenantRepository implements TenantRepositoryPort {

    private final Map<String, Tenant> store = new ConcurrentHashMap<>();

    @Override
    public void save(Tenant tenant) {
        store.put(tenant.tenantId().value(), tenant);
    }

    @Override
    public Optional<Tenant> findByTenantId(TenantId tenantId) {
        return Optional.ofNullable(store.get(tenantId.value()));
    }
}
