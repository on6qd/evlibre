CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    company_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
