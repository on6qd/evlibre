package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.server.core.domain.dto.AuthorizationResult;
import com.evlibre.server.core.domain.model.TenantId;

public interface AuthorizePort {

    AuthorizationResult authorize(TenantId tenantId, String idTag);
}
