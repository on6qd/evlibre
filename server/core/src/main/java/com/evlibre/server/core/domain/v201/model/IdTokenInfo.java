package com.evlibre.server.core.domain.v201.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * OCPP 2.0.1 {@code IdTokenInfoType} — the authorization verdict and policy
 * carried alongside an {@link IdToken} (in Authorize responses, SendLocalList
 * entries, and cache population).
 *
 * <p>When {@link #status} is {@link AuthorizationStatus#ACCEPTED} the other
 * fields express policy; otherwise they are typically ignored. All optional
 * fields may be {@code null}.
 *
 * <p>{@code chargingPriority} is spec-bounded to [-9, 9]; we validate it here
 * because a violation would cause the station to reject the payload.
 * {@code evseId} is optional; when non-null it restricts the token to the
 * listed EVSEs.
 */
public record IdTokenInfo(
        AuthorizationStatus status,
        Instant cacheExpiryDateTime,
        Integer chargingPriority,
        List<Integer> evseId,
        IdToken groupIdToken,
        String language1,
        String language2,
        MessageContent personalMessage) {

    public IdTokenInfo {
        Objects.requireNonNull(status, "status");
        if (chargingPriority != null && (chargingPriority < -9 || chargingPriority > 9)) {
            throw new IllegalArgumentException(
                    "chargingPriority must be in [-9, 9], got " + chargingPriority);
        }
        if (evseId != null) {
            if (evseId.isEmpty()) {
                throw new IllegalArgumentException(
                        "evseId must be null or contain at least one entry");
            }
            evseId = List.copyOf(evseId);
        }
    }

    public static IdTokenInfo accepted() {
        return new IdTokenInfo(
                AuthorizationStatus.ACCEPTED, null, null, null, null, null, null, null);
    }

    public static IdTokenInfo of(AuthorizationStatus status) {
        return new IdTokenInfo(status, null, null, null, null, null, null, null);
    }
}
