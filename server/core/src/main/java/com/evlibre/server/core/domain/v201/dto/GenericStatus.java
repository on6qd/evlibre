package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code GenericStatusEnumType} — simple yes/no outcome used by
 * multiple spec messages (GetCompositeSchedule, NotifyAllowedEnergyTransfer,
 * PublishFirmware, …). Kept in the shared {@code dto/} package so every
 * response DTO that uses it can depend on the same spelling.
 */
public enum GenericStatus {
    ACCEPTED,
    REJECTED
}
