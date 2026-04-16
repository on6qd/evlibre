package com.evlibre.server.adapter.persistence.h2;

import com.evlibre.server.core.domain.model.AuthorizationStatus;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.AuthorizationRepositoryPort;

import java.sql.*;
import java.util.Optional;

public class H2AuthorizationRepository implements AuthorizationRepositoryPort {

    private final H2DatabaseManager db;

    public H2AuthorizationRepository(H2DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Optional<AuthorizationStatus> findStatusByIdTag(TenantId tenantId, String idTag) {
        // OCPP 1.6: IdToken is CiString20Type — case-insensitive.
        String sql = "SELECT status FROM authorizations WHERE tenant_id = ? AND LOWER(id_tag) = LOWER(?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId.value());
            ps.setString(2, idTag);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(AuthorizationStatus.valueOf(rs.getString("status")));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find authorization", e);
        }
    }
}
