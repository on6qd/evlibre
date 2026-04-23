package com.evlibre.server.core.domain.v201.diagnostics;

/**
 * OCPP 2.0.1 {@code LogStatusEnumType} — response to a {@code GetLog} call.
 *
 * <ul>
 *   <li>{@link #ACCEPTED} — log will be uploaded.</li>
 *   <li>{@link #REJECTED} — station refuses (e.g. log type unsupported).</li>
 *   <li>{@link #ACCEPTED_CANCELED} — station cancelled an in-flight upload to
 *       honour this new request (N01.FR.12).</li>
 * </ul>
 */
public enum GetLogStatus {
    ACCEPTED,
    REJECTED,
    ACCEPTED_CANCELED
}
