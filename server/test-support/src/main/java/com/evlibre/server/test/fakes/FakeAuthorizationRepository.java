package com.evlibre.server.test.fakes;

import com.evlibre.server.core.domain.v16.model.AuthorizationStatus;
import com.evlibre.server.core.domain.v16.model.IdTagInfo;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.outbound.AuthorizationRepositoryPort;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class FakeAuthorizationRepository implements AuthorizationRepositoryPort {

    private final Map<String, IdTagInfo> store = new ConcurrentHashMap<>();

    @Override
    public Optional<IdTagInfo> findInfoByIdTag(TenantId tenantId, String idTag) {
        return Optional.ofNullable(store.get(key(tenantId, idTag)));
    }

    public void addAuthorization(TenantId tenantId, String idTag, AuthorizationStatus status) {
        store.put(key(tenantId, idTag), IdTagInfo.of(status));
    }

    public void addAuthorization(TenantId tenantId, String idTag, IdTagInfo info) {
        store.put(key(tenantId, idTag), info);
    }

    public void clear() {
        store.clear();
    }

    // OCPP 1.6: IdToken is CiString20Type — case-insensitive.
    private String key(TenantId tenantId, String idTag) {
        return tenantId.value() + ":" + (idTag == null ? "" : idTag.toLowerCase(Locale.ROOT));
    }
}
