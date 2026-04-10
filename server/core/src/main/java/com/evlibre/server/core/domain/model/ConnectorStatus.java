package com.evlibre.server.core.domain.model;

public enum ConnectorStatus {
    AVAILABLE,
    PREPARING,
    CHARGING,
    SUSPENDED_EV,
    SUSPENDED_EVSE,
    FINISHING,
    RESERVED,
    UNAVAILABLE,
    FAULTED
}
