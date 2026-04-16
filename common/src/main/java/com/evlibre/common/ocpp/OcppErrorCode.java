package com.evlibre.common.ocpp;

public enum OcppErrorCode {

    NOT_IMPLEMENTED("NotImplemented"),
    NOT_SUPPORTED("NotSupported"),
    INTERNAL_ERROR("InternalError"),
    PROTOCOL_ERROR("ProtocolError"),
    SECURITY_ERROR("SecurityError"),
    FORMATION_VIOLATION("FormationViolation"),
    PROPERTY_CONSTRAINT_VIOLATION("PropertyConstraintViolation"),
    // OCPP 1.6 errata mandates the misspelled form "Occurence" (single r)
    // and explicitly forbids fixing it to preserve interoperability.
    OCCURENCE_CONSTRAINT_VIOLATION("OccurenceConstraintViolation"),
    TYPE_CONSTRAINT_VIOLATION("TypeConstraintViolation"),
    GENERIC_ERROR("GenericError");

    private final String value;

    OcppErrorCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
