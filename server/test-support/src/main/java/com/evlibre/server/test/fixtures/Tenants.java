package com.evlibre.server.test.fixtures;

import com.evlibre.server.core.domain.shared.model.Tenant;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.time.Instant;
import java.util.UUID;

public final class Tenants {

    public static final TenantId DEMO_TENANT_ID = new TenantId("demo-tenant");
    public static final Instant DEFAULT_TIME = Instant.parse("2025-01-15T10:00:00Z");

    private Tenants() {}

    public static Tenant demo() {
        return Tenant.builder()
                .id(UUID.randomUUID())
                .tenantId(DEMO_TENANT_ID)
                .companyName("Demo Company")
                .createdAt(DEFAULT_TIME)
                .build();
    }

    public static Tenant withId(String tenantId) {
        return Tenant.builder()
                .id(UUID.randomUUID())
                .tenantId(new TenantId(tenantId))
                .companyName(tenantId + " Company")
                .createdAt(DEFAULT_TIME)
                .build();
    }
}
