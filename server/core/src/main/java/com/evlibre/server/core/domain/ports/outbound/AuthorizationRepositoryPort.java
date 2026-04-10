package com.evlibre.server.core.domain.ports.outbound;

import com.evlibre.server.core.domain.model.AuthorizationStatus;
import com.evlibre.server.core.domain.model.TenantId;

import java.util.Optional;

public interface AuthorizationRepositoryPort {

    Optional<AuthorizationStatus> findStatusByIdTag(TenantId tenantId, String idTag);
}
