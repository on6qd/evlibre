package com.evlibre.server.core.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantTest {

    @Test
    void build_valid_tenant() {
        var tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .tenantId(new TenantId("demo-tenant"))
                .companyName("Demo Company")
                .createdAt(Instant.now())
                .build();

        assertThat(tenant.tenantId().value()).isEqualTo("demo-tenant");
        assertThat(tenant.companyName()).isEqualTo("Demo Company");
    }

    @Test
    void missing_required_fields_throws() {
        assertThatThrownBy(() -> Tenant.builder().build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void tenantId_null_throws() {
        assertThatThrownBy(() -> new TenantId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void tenantId_blank_throws() {
        assertThatThrownBy(() -> new TenantId(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
