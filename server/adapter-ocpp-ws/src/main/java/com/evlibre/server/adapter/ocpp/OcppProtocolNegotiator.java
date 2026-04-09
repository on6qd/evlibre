package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.ocpp.OcppProtocol;

import java.util.List;

public class OcppProtocolNegotiator {

    private static final List<OcppProtocol> PREFERRED_ORDER = List.of(
            OcppProtocol.OCPP_201,
            OcppProtocol.OCPP_16
    );

    /**
     * Negotiate the best OCPP protocol from the offered sub-protocols.
     * Returns null if no match found.
     */
    public OcppProtocol negotiate(List<String> offeredSubProtocols) {
        if (offeredSubProtocols == null || offeredSubProtocols.isEmpty()) {
            return null;
        }

        for (OcppProtocol preferred : PREFERRED_ORDER) {
            if (offeredSubProtocols.contains(preferred.subProtocol())) {
                return preferred;
            }
        }
        return null;
    }
}
