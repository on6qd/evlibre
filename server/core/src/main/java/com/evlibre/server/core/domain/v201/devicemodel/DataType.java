package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code DataEnumType}: the data type of a variable's value.
 * Referenced by {@link VariableCharacteristics}.
 *
 * <p>Wire form uses the spec's mixed-case tokens ({@code string},
 * {@code decimal}, {@code integer}, {@code dateTime}, {@code boolean},
 * {@code OptionList}, {@code SequenceList}, {@code MemberList}); the adapter
 * layer maps to/from these constants.
 */
public enum DataType {
    /** Standard UTF-8 string. */
    STRING,
    /** Numeric value with fractional part (CSMS honours up to 6 decimal places). */
    DECIMAL,
    /** 32-bit signed integer. */
    INTEGER,
    /** Timestamp in RFC 3339 format. */
    DATE_TIME,
    /** Boolean logic (true/false). */
    BOOLEAN,
    /** Single choice from a provided list. */
    OPTION_LIST,
    /** Ordered set of values from a provided list. */
    SEQUENCE_LIST,
    /** Unordered (mathematical) set of values from a list. */
    MEMBER_LIST
}
