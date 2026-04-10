CREATE TABLE charging_stations (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    station_identity VARCHAR(255) NOT NULL,
    protocol_version VARCHAR(20) NOT NULL,
    vendor VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    serial_number VARCHAR(100),
    firmware_version VARCHAR(100),
    registration_status VARCHAR(20),
    last_boot_notification TIMESTAMP,
    last_heartbeat TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_stations_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT uq_station_per_tenant UNIQUE (tenant_id, station_identity)
);

CREATE INDEX idx_stations_tenant ON charging_stations(tenant_id);
