package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.server.core.domain.model.AuthorizationStatus;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.AuthorizationRepositoryPort;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAuthorizationRepository implements AuthorizationRepositoryPort {

    private final Map<String, AuthorizationStatus> store = new ConcurrentHashMap<>();

    @Override
    public Optional<AuthorizationStatus> findStatusByIdTag(TenantId tenantId, String idTag) {
        return Optional.ofNullable(store.get(key(tenantId, idTag)));
    }

    public void addAuthorization(TenantId tenantId, String idTag, AuthorizationStatus status) {
        store.put(key(tenantId, idTag), status);
    }

    private String key(TenantId tenantId, String idTag) {
        return tenantId.value() + ":" + idTag;
    }
}
