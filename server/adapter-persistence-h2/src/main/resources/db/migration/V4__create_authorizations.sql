CREATE TABLE authorizations (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    id_tag VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
    expiry_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_auth_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT uq_auth_per_tenant UNIQUE (tenant_id, id_tag)
);
