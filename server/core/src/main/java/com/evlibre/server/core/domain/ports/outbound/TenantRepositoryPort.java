package com.evlibre.server.core.domain.ports.outbound;

import com.evlibre.server.core.domain.model.Tenant;
import com.evlibre.server.core.domain.model.TenantId;

import java.util.Optional;

public interface TenantRepositoryPort {

    void save(Tenant tenant);

    Optional<Tenant> findByTenantId(TenantId tenantId);
}
