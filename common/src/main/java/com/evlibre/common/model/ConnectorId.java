package com.evlibre.common.model;

/**
 * OCPP connector identifier. Connector 0 represents the charge point main controller.
 * Physical connectors start at 1.
 */
public record ConnectorId(int value) {

    public ConnectorId {
        if (value < 0) {
            throw new IllegalArgumentException("Connector ID must not be negative");
        }
    }

    public boolean isMainController() {
        return value == 0;
    }
}
