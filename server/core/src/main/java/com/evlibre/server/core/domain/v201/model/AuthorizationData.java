package com.evlibre.server.core.domain.v201.model;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code AuthorizationData} — a single entry in a
 * {@code SendLocalListRequest}'s {@code localAuthorizationList}.
 *
 * <p>The {@link #idTokenInfo} field's presence/absence is semantically
 * load-bearing under a {@code Differential} update:
 * <ul>
 *   <li>Present → add or update the entry (D01.FR.16).</li>
 *   <li>Absent → remove the entry from the station's list (D01.FR.17).</li>
 * </ul>
 * Under a {@code Full} update, entries without {@code idTokenInfo} are not
 * meaningful; the whole list is replaced atomically (D01.FR.15).
 */
public record AuthorizationData(IdToken idToken, IdTokenInfo idTokenInfo) {

    public AuthorizationData {
        Objects.requireNonNull(idToken, "idToken");
    }

    public static AuthorizationData add(IdToken idToken, IdTokenInfo idTokenInfo) {
        Objects.requireNonNull(idTokenInfo, "idTokenInfo must be non-null for an add/update entry");
        return new AuthorizationData(idToken, idTokenInfo);
    }

    public static AuthorizationData remove(IdToken idToken) {
        return new AuthorizationData(idToken, null);
    }
}
