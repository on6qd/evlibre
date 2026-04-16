package com.evlibre.server.core.domain.model;

import java.time.Instant;

/**
 * OCPP 1.6 IdTagInfo (§7.8): status plus optional expiryDate and parentIdTag.
 * Returned in Authorize.conf, StartTransaction.conf, and StopTransaction.conf.
 */
public record IdTagInfo(
        AuthorizationStatus status,
        Instant expiryDate,
        String parentIdTag
) {
    public static IdTagInfo of(AuthorizationStatus status) {
        return new IdTagInfo(status, null, null);
    }
}
