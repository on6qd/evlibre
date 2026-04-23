package com.evlibre.server.core.domain.v201.model;

/**
 * OCPP 2.0.1 {@code UpdateEnumType} — how the station should integrate the
 * list in a {@code SendLocalListRequest}.
 *
 * <ul>
 *   <li>{@link #FULL} — replace the entire Local Authorization List with the
 *       supplied entries (empty list clears the list; D01.FR.04 / D01.FR.15).</li>
 *   <li>{@link #DIFFERENTIAL} — add/update entries that carry {@code idTokenInfo}
 *       (D01.FR.16); remove entries that omit it (D01.FR.17).</li>
 * </ul>
 */
public enum UpdateType {
    DIFFERENTIAL,
    FULL
}
