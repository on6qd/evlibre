package com.evlibre.server.core.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Tenant {

    private final UUID id;
    private final TenantId tenantId;
    private final String companyName;
    private final Instant createdAt;

    private Tenant(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id required");
        this.tenantId = Objects.requireNonNull(builder.tenantId, "tenantId required");
        this.companyName = Objects.requireNonNull(builder.companyName, "companyName required");
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt required");
    }

    public UUID id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public String companyName() { return companyName; }
    public Instant createdAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private TenantId tenantId;
        private String companyName;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tenantId(TenantId tenantId) { this.tenantId = tenantId; return this; }
        public Builder companyName(String companyName) { this.companyName = companyName; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public Builder from(Tenant tenant) {
            this.id = tenant.id;
            this.tenantId = tenant.tenantId;
            this.companyName = tenant.companyName;
            this.createdAt = tenant.createdAt;
            return this;
        }

        public Tenant build() { return new Tenant(this); }
    }
}
