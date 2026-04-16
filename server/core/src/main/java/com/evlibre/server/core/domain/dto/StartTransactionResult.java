package com.evlibre.server.core.domain.dto;

import com.evlibre.server.core.domain.model.AuthorizationStatus;

import java.time.Instant;

public record StartTransactionResult(
        int transactionId,
        AuthorizationStatus idTagStatus,
        Instant expiryDate,
        String parentIdTag
) {
    public StartTransactionResult(int transactionId, AuthorizationStatus idTagStatus) {
        this(transactionId, idTagStatus, null, null);
    }

    public AuthorizationResult toAuthorizationResult(String idTag) {
        return new AuthorizationResult(idTagStatus, idTag, expiryDate, parentIdTag);
    }
}
