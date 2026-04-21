package com.evlibre.server.core.domain.v16.ports.outbound;

import com.evlibre.server.core.domain.v16.model.AuthorizationStatus;
import com.evlibre.server.core.domain.v16.model.IdTagInfo;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.util.Optional;

public interface AuthorizationRepositoryPort {

    /**
     * Full IdTagInfo lookup per OCPP 1.6 §7.8 (status, expiryDate, parentIdTag).
     */
    Optional<IdTagInfo> findInfoByIdTag(TenantId tenantId, String idTag);

    /**
     * Convenience delegating to {@link #findInfoByIdTag} for callers that only need the status.
     */
    default Optional<AuthorizationStatus> findStatusByIdTag(TenantId tenantId, String idTag) {
        return findInfoByIdTag(tenantId, idTag).map(IdTagInfo::status);
    }
}
