package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.server.core.domain.shared.model.Tenant;
import com.evlibre.server.core.domain.shared.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTenantRepositoryTest {

    private InMemoryTenantRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryTenantRepository();
    }

    @Test
    void save_and_find() {
        var tenant = aTenant("demo-tenant");
        repo.save(tenant);

        assertThat(repo.findByTenantId(new TenantId("demo-tenant"))).contains(tenant);
    }

    @Test
    void findByTenantId_notFound() {
        assertThat(repo.findByTenantId(new TenantId("unknown"))).isEmpty();
    }

    private Tenant aTenant(String id) {
        return Tenant.builder()
                .id(UUID.randomUUID())
                .tenantId(new TenantId(id))
                .companyName("Company")
                .createdAt(Instant.now())
                .build();
    }
}
