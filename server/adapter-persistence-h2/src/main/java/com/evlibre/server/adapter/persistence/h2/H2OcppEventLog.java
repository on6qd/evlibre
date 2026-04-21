package com.evlibre.server.adapter.persistence.h2;

import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;

import java.sql.*;
import java.util.UUID;

public class H2OcppEventLog implements OcppEventLogPort {

    private final H2DatabaseManager db;

    public H2OcppEventLog(H2DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void logEvent(String stationIdentity, String messageId, String action,
                         String direction, String payload) {
        String sql = "INSERT INTO ocpp_events (id, station_identity, message_id, action, direction, payload, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, stationIdentity);
            ps.setString(3, messageId);
            ps.setString(4, action);
            ps.setString(5, direction);
            ps.setString(6, payload);
            ps.setTimestamp(7, Timestamp.from(java.time.Instant.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to log OCPP event", e);
        }
    }
}
