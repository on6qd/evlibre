package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.server.core.domain.v16.dto.AuthorizationResult;
import com.evlibre.server.core.domain.shared.model.TenantId;

public interface AuthorizePort {

    AuthorizationResult authorize(TenantId tenantId, String idTag);
}
