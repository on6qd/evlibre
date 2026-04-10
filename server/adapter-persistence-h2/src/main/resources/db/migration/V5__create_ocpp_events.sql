CREATE TABLE ocpp_events (
    id UUID PRIMARY KEY,
    station_identity VARCHAR(255) NOT NULL,
    message_id VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    payload TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_ocpp_events_station ON ocpp_events(station_identity);
CREATE INDEX idx_ocpp_events_action ON ocpp_events(action);
