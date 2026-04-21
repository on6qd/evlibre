package com.evlibre.server.adapter.persistence.h2;

import com.evlibre.server.core.domain.shared.model.Tenant;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.TenantRepositoryPort;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class H2TenantRepository implements TenantRepositoryPort {

    private final H2DatabaseManager db;

    public H2TenantRepository(H2DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void save(Tenant tenant) {
        String sql = "MERGE INTO tenants (id, tenant_id, company_name, created_at) KEY(tenant_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, tenant.id());
            ps.setString(2, tenant.tenantId().value());
            ps.setString(3, tenant.companyName());
            ps.setTimestamp(4, Timestamp.from(tenant.createdAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save tenant", e);
        }
    }

    @Override
    public Optional<Tenant> findByTenantId(TenantId tenantId) {
        String sql = "SELECT id, tenant_id, company_name, created_at FROM tenants WHERE tenant_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId.value());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapTenant(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find tenant", e);
        }
    }

    private Tenant mapTenant(ResultSet rs) throws SQLException {
        return Tenant.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(new TenantId(rs.getString("tenant_id")))
                .companyName(rs.getString("company_name"))
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .build();
    }
}
