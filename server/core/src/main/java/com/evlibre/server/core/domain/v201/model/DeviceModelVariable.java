package com.evlibre.server.core.domain.v201.model;

/**
 * OCPP 2.0.1 Device Model variable from NotifyReport.
 * Represents a single variable within the charger's device model.
 */
public record DeviceModelVariable(
        String componentName,
        String componentInstance,
        Integer evseId,
        Integer connectorId,
        String variableName,
        String variableInstance,
        String attributeType,
        String value,
        String dataType,
        boolean supportsMonitoring
) {}
