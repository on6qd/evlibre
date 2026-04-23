package com.evlibre.server.core.domain.v201.model;

/**
 * OCPP 2.0.1 {@code ConnectorEnumType} — physical connector / plug form-factor.
 *
 * <p>Used by {@code ReserveNow} to narrow a reservation to a specific plug type
 * (e.g. only reserve a CCS2 cable), and by the device model's
 * {@code ConnectorType} variable to describe what a {@link Evse}-scoped
 * {@code Connector} exposes.
 *
 * <p>The enum intentionally preserves the spec's exact wire spellings via
 * {@link #wire()}; those are hand-rolled rather than derived from
 * {@link Enum#name()} because the spec mixes lowercase prefixes (c/s/w) with
 * mixed-case bodies that don't survive uppercase Java-enum convention
 * round-tripping.
 */
public enum ConnectorType {
    CCCS1("cCCS1"),
    CCCS2("cCCS2"),
    CG105("cG105"),
    CTESLA("cTesla"),
    CTYPE1("cType1"),
    CTYPE2("cType2"),
    CGBT("cGBT"),
    CCHAOJI("cChaoJi"),
    STYPE2("sType2"),
    STYPE3("sType3"),
    S309_1P_16A("s309-1P-16A"),
    S309_1P_32A("s309-1P-32A"),
    S309_3P_16A("s309-3P-16A"),
    S309_3P_32A("s309-3P-32A"),
    SBS1361("sBS1361"),
    SCEE_7_7("sCEE-7-7"),
    PAN("Pan"),
    OPP_CHARGE("OppCharge"),
    W_INDUCTIVE("wInductive"),
    W_RESONANT("wResonant"),
    OTHER_1PH_MAX_16A("Other1PhMax16A"),
    OTHER_1PH_OVER_16A("Other1PhOver16A"),
    OTHER_3PH("Other3Ph"),
    UNDETERMINED("Undetermined"),
    UNKNOWN("Unknown");

    private final String wire;

    ConnectorType(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }
}
