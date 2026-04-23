package com.evlibre.server.core.domain.v201.network;

/**
 * OCPP 2.0.1 {@code OCPPTransportEnumType}: transport protocol. Only
 * {@link #JSON} is in scope for v2.0.1; {@link #SOAP} exists in the spec for
 * backwards-compatible profiles that target older CSMS endpoints.
 */
public enum OcppTransport {
    JSON,
    SOAP
}
