package com.evlibre.server.adapter.persistence.h2;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.model.Transaction;
import com.evlibre.server.core.domain.model.TransactionStatus;
import com.evlibre.server.core.domain.ports.outbound.TransactionRepositoryPort;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class H2TransactionRepository implements TransactionRepositoryPort {

    private final H2DatabaseManager db;

    public H2TransactionRepository(H2DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void save(Transaction tx) {
        String sql = """
                MERGE INTO transactions (id, ocpp_transaction_id, station_id, tenant_id,
                    station_identity, connector_id, id_tag, start_time, stop_time,
                    meter_start, meter_stop, status, stop_reason, created_at)
                KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, tx.id());
            ps.setInt(2, tx.ocppTransactionId());
            ps.setObject(3, tx.stationId());
            ps.setString(4, tx.tenantId().value());
            ps.setString(5, tx.stationIdentity().value());
            ps.setInt(6, tx.connectorId().value());
            ps.setString(7, tx.idTag());
            ps.setTimestamp(8, Timestamp.from(tx.startTime()));
            ps.setTimestamp(9, tx.stopTime() != null ? Timestamp.from(tx.stopTime()) : null);
            ps.setLong(10, tx.meterStart());
            ps.setLong(11, tx.meterStop());
            ps.setString(12, tx.status().name());
            ps.setString(13, tx.stopReason());
            ps.setTimestamp(14, Timestamp.from(tx.createdAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save transaction", e);
        }
    }

    @Override
    public Optional<Transaction> findByOcppTransactionId(TenantId tenantId, int ocppTransactionId) {
        String sql = "SELECT * FROM transactions WHERE tenant_id = ? AND ocpp_transaction_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId.value());
            ps.setInt(2, ocppTransactionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapTransaction(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find transaction", e);
        }
    }

    @Override
    public List<Transaction> findByStation(UUID stationId) {
        String sql = "SELECT * FROM transactions WHERE station_id = ? ORDER BY start_time DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, stationId);
            ResultSet rs = ps.executeQuery();
            List<Transaction> txs = new ArrayList<>();
            while (rs.next()) txs.add(mapTransaction(rs));
            return txs;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find transactions", e);
        }
    }

    @Override
    public Optional<Transaction> findActiveByIdTag(TenantId tenantId, String idTag) {
        if (idTag == null) return Optional.empty();
        // OCPP 1.6: IdToken is CiString20Type — case-insensitive.
        String sql = "SELECT * FROM transactions WHERE tenant_id = ? AND LOWER(id_tag) = LOWER(?) "
                + "AND status = 'IN_PROGRESS' LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId.value());
            ps.setString(2, idTag);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapTransaction(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active transaction by idTag", e);
        }
    }

    @Override
    public int nextOcppTransactionId() {
        String sql = "SELECT NEXT VALUE FOR transaction_id_seq";
        try (Connection conn = db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get next transaction ID", e);
        }
    }

    private Transaction mapTransaction(ResultSet rs) throws SQLException {
        Timestamp stopTime = rs.getTimestamp("stop_time");
        return Transaction.builder()
                .id(rs.getObject("id", UUID.class))
                .ocppTransactionId(rs.getInt("ocpp_transaction_id"))
                .stationId(rs.getObject("station_id", UUID.class))
                .tenantId(new TenantId(rs.getString("tenant_id")))
                .stationIdentity(new ChargePointIdentity(rs.getString("station_identity")))
                .connectorId(new ConnectorId(rs.getInt("connector_id")))
                .idTag(rs.getString("id_tag"))
                .startTime(rs.getTimestamp("start_time").toInstant())
                .stopTime(stopTime != null ? stopTime.toInstant() : null)
                .meterStart(rs.getLong("meter_start"))
                .meterStop(rs.getLong("meter_stop"))
                .status(TransactionStatus.valueOf(rs.getString("status")))
                .stopReason(rs.getString("stop_reason"))
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .build();
    }
}
