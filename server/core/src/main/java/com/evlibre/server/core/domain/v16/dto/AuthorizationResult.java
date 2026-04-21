package com.evlibre.server.core.domain.v16.dto;

import com.evlibre.server.core.domain.v16.model.AuthorizationStatus;
import com.evlibre.server.core.domain.v16.model.IdTagInfo;

import java.time.Instant;

public record AuthorizationResult(
        AuthorizationStatus status,
        String idTag,
        Instant expiryDate,
        String parentIdTag
) {
    public AuthorizationResult(AuthorizationStatus status, String idTag) {
        this(status, idTag, null, null);
    }

    public static AuthorizationResult from(IdTagInfo info, String idTag) {
        return new AuthorizationResult(info.status(), idTag, info.expiryDate(), info.parentIdTag());
    }
}
