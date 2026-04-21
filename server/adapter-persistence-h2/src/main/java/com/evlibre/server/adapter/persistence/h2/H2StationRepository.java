package com.evlibre.server.adapter.persistence.h2;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.core.domain.shared.model.ChargingStation;
import com.evlibre.server.core.domain.shared.model.RegistrationStatus;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class H2StationRepository implements StationRepositoryPort {

    private final H2DatabaseManager db;

    public H2StationRepository(H2DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void save(ChargingStation station) {
        String sql = """
                MERGE INTO charging_stations (id, tenant_id, station_identity, protocol_version,
                    vendor, model, serial_number, firmware_version, registration_status,
                    last_boot_notification, last_heartbeat, created_at)
                KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, station.id());
            ps.setString(2, station.tenantId().value());
            ps.setString(3, station.identity().value());
            ps.setString(4, station.protocol().name());
            ps.setString(5, station.vendor());
            ps.setString(6, station.model());
            ps.setString(7, station.serialNumber());
            ps.setString(8, station.firmwareVersion());
            ps.setString(9, station.registrationStatus() != null ? station.registrationStatus().name() : null);
            ps.setTimestamp(10, station.lastBootNotification() != null ? Timestamp.from(station.lastBootNotification()) : null);
            ps.setTimestamp(11, station.lastHeartbeat() != null ? Timestamp.from(station.lastHeartbeat()) : null);
            ps.setTimestamp(12, Timestamp.from(station.createdAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save station", e);
        }
    }

    @Override
    public Optional<ChargingStation> findById(UUID id) {
        String sql = "SELECT * FROM charging_stations WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapStation(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find station", e);
        }
    }

    @Override
    public Optional<ChargingStation> findByTenantAndIdentity(TenantId tenantId, ChargePointIdentity identity) {
        String sql = "SELECT * FROM charging_stations WHERE tenant_id = ? AND station_identity = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId.value());
            ps.setString(2, identity.value());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapStation(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find station", e);
        }
    }

    @Override
    public List<ChargingStation> findByTenant(TenantId tenantId) {
        String sql = "SELECT * FROM charging_stations WHERE tenant_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId.value());
            ResultSet rs = ps.executeQuery();
            List<ChargingStation> stations = new ArrayList<>();
            while (rs.next()) stations.add(mapStation(rs));
            return stations;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find stations", e);
        }
    }

    private ChargingStation mapStation(ResultSet rs) throws SQLException {
        String regStatus = rs.getString("registration_status");
        Timestamp lastBoot = rs.getTimestamp("last_boot_notification");
        Timestamp lastHb = rs.getTimestamp("last_heartbeat");

        return ChargingStation.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(new TenantId(rs.getString("tenant_id")))
                .identity(new ChargePointIdentity(rs.getString("station_identity")))
                .protocol(OcppProtocol.valueOf(rs.getString("protocol_version")))
                .vendor(rs.getString("vendor"))
                .model(rs.getString("model"))
                .serialNumber(rs.getString("serial_number"))
                .firmwareVersion(rs.getString("firmware_version"))
                .registrationStatus(regStatus != null ? RegistrationStatus.valueOf(regStatus) : null)
                .lastBootNotification(lastBoot != null ? lastBoot.toInstant() : null)
                .lastHeartbeat(lastHb != null ? lastHb.toInstant() : null)
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .build();
    }
}
