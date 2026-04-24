package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code GenericDeviceModelStatusEnumType} — outcome enum used by
 * device-model messages that may legitimately resolve to "nothing to return"
 * or "not supported by this station". Shared across {@code GetBaseReport},
 * {@code GetReport}, {@code GetMonitoringReport}, {@code SetMonitoringBase},
 * and {@code CustomerInformation} so every use case emits the same spelling.
 *
 * <p>Distinct from {@link GenericStatus} (2 values): that one is used where
 * the spec only allows Accepted/Rejected (e.g. {@code PublishFirmware},
 * {@code GetCompositeSchedule}).
 */
public enum GenericDeviceModelStatus {
    ACCEPTED,
    REJECTED,
    NOT_SUPPORTED,
    EMPTY_RESULT_SET
}
