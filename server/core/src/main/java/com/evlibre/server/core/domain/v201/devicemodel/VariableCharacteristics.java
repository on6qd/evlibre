package com.evlibre.server.core.domain.v201.devicemodel;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * OCPP 2.0.1 {@code VariableCharacteristicsType}: fixed, read-only meta-data
 * describing a {@link Variable} — its unit, data type, limits, permitted
 * value list (for OptionList / SequenceList / MemberList types), and whether
 * it is monitorable.
 */
public record VariableCharacteristics(
        String unit,
        DataType dataType,
        BigDecimal minLimit,
        BigDecimal maxLimit,
        String valuesList,
        boolean supportsMonitoring) {

    private static final int UNIT_MAX = 16;
    private static final int VALUES_LIST_MAX = 1000;

    public VariableCharacteristics {
        Objects.requireNonNull(dataType, "VariableCharacteristics.dataType must not be null");
        if (unit != null && unit.length() > UNIT_MAX) {
            throw new IllegalArgumentException(
                    "VariableCharacteristics.unit must be <= " + UNIT_MAX + " chars, got " + unit.length());
        }
        if (valuesList != null && valuesList.length() > VALUES_LIST_MAX) {
            throw new IllegalArgumentException(
                    "VariableCharacteristics.valuesList must be <= " + VALUES_LIST_MAX + " chars, got " + valuesList.length());
        }
    }

    public static VariableCharacteristics of(DataType dataType, boolean supportsMonitoring) {
        return new VariableCharacteristics(null, dataType, null, null, null, supportsMonitoring);
    }
}
