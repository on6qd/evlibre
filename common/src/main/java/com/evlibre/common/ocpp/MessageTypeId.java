package com.evlibre.common.ocpp;

public enum MessageTypeId {

    CALL(2),
    CALL_RESULT(3),
    CALL_ERROR(4);

    private final int value;

    MessageTypeId(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static MessageTypeId fromValue(int value) {
        for (MessageTypeId type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }
}
