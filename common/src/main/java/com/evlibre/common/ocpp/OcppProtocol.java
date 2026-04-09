package com.evlibre.common.ocpp;

public enum OcppProtocol {

    OCPP_16("ocpp1.6"),
    OCPP_201("ocpp2.0.1");

    private final String subProtocol;

    OcppProtocol(String subProtocol) {
        this.subProtocol = subProtocol;
    }

    public String subProtocol() {
        return subProtocol;
    }

    public static OcppProtocol fromSubProtocol(String subProtocol) {
        for (OcppProtocol protocol : values()) {
            if (protocol.subProtocol.equals(subProtocol)) {
                return protocol;
            }
        }
        return null;
    }
}
