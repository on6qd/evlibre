package com.evlibre.server.core.domain.shared.ports.outbound;

import com.evlibre.server.core.domain.shared.model.Tenant;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.util.Optional;

public interface TenantRepositoryPort {

    void save(Tenant tenant);

    Optional<Tenant> findByTenantId(TenantId tenantId);
}
