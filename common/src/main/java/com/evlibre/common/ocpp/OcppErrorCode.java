package com.evlibre.common.ocpp;

public enum OcppErrorCode {

    NOT_IMPLEMENTED("NotImplemented"),
    NOT_SUPPORTED("NotSupported"),
    INTERNAL_ERROR("InternalError"),
    PROTOCOL_ERROR("ProtocolError"),
    SECURITY_ERROR("SecurityError"),
    FORMATION_VIOLATION("FormationViolation"),
    PROPERTY_CONSTRAINT_VIOLATION("PropertyConstraintViolation"),
    OCCURRENCE_CONSTRAINT_VIOLATION("OccurrenceConstraintViolation"),
    TYPE_CONSTRAINT_VIOLATION("TypeConstraintViolation"),
    GENERIC_ERROR("GenericError"),
    MESSAGE_TYPE_NOT_SUPPORTED("MessageTypeNotSupported");

    private final String value;

    OcppErrorCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
