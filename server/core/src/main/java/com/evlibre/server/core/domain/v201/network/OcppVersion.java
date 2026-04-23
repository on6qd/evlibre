package com.evlibre.server.core.domain.v201.network;

/**
 * OCPP 2.0.1 {@code OCPPVersionEnumType}: protocol version carried in a
 * {@link NetworkConnectionProfile}.
 *
 * <p>Wire form is {@code OCPP12}, {@code OCPP15}, {@code OCPP16}, {@code OCPP20}
 * (the spec reuses {@code OCPP20} for v2.0.1).
 */
public enum OcppVersion {
    OCPP_12,
    OCPP_15,
    OCPP_16,
    OCPP_20
}
