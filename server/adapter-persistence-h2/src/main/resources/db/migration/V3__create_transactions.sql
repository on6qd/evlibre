CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    ocpp_transaction_id INTEGER NOT NULL,
    station_id UUID NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    station_identity VARCHAR(255) NOT NULL,
    connector_id INTEGER NOT NULL,
    id_tag VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    stop_time TIMESTAMP,
    meter_start BIGINT NOT NULL,
    meter_stop BIGINT,
    status VARCHAR(30) NOT NULL,
    stop_reason VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_tx_station FOREIGN KEY (station_id) REFERENCES charging_stations(id)
);

CREATE SEQUENCE transaction_id_seq START WITH 1 INCREMENT BY 1;
CREATE INDEX idx_tx_station ON transactions(station_id);
CREATE INDEX idx_tx_tenant ON transactions(tenant_id);
CREATE INDEX idx_tx_status ON transactions(status);
