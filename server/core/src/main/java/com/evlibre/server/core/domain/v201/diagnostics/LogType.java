package com.evlibre.server.core.domain.v201.diagnostics;

/**
 * OCPP 2.0.1 {@code LogEnumType} — which log set the CSMS asks the station to
 * upload via {@code GetLog} (block N01).
 *
 * <ul>
 *   <li>{@link #DIAGNOSTICS_LOG} — general diagnostics, replaces 1.6's
 *       {@code GetDiagnostics}.</li>
 *   <li>{@link #SECURITY_LOG} — security event log (TLS handshakes,
 *       authentication failures, certificate operations).</li>
 * </ul>
 */
public enum LogType {
    DIAGNOSTICS_LOG,
    SECURITY_LOG
}
