INSERT INTO tenants (id, tenant_id, company_name, created_at) VALUES
  (RANDOM_UUID(), 'demo-tenant', 'Demo Company', CURRENT_TIMESTAMP);

INSERT INTO authorizations (id, tenant_id, id_tag, status, created_at) VALUES
  (RANDOM_UUID(), 'demo-tenant', 'TAG001', 'ACCEPTED', CURRENT_TIMESTAMP),
  (RANDOM_UUID(), 'demo-tenant', 'TAG002', 'ACCEPTED', CURRENT_TIMESTAMP);
