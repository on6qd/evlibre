package com.evlibre.server.adapter.persistence.h2;

import com.evlibre.server.core.domain.v16.model.AuthorizationStatus;
import com.evlibre.server.core.domain.v16.model.IdTagInfo;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.outbound.AuthorizationRepositoryPort;

import java.sql.*;
import java.util.Optional;

public class H2AuthorizationRepository implements AuthorizationRepositoryPort {

    private final H2DatabaseManager db;

    public H2AuthorizationRepository(H2DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Optional<IdTagInfo> findInfoByIdTag(TenantId tenantId, String idTag) {
        // OCPP 1.6: IdToken is CiString20Type — case-insensitive.
        String sql = "SELECT status, expiry_date, parent_id_tag FROM authorizations "
                + "WHERE tenant_id = ? AND LOWER(id_tag) = LOWER(?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId.value());
            ps.setString(2, idTag);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                AuthorizationStatus status = AuthorizationStatus.valueOf(rs.getString("status"));
                Timestamp expiry = rs.getTimestamp("expiry_date");
                String parentIdTag = rs.getString("parent_id_tag");
                return Optional.of(new IdTagInfo(
                        status,
                        expiry != null ? expiry.toInstant() : null,
                        parentIdTag
                ));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find authorization", e);
        }
    }
}
