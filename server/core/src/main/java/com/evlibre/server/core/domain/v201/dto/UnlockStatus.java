package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code UnlockStatusEnumType} — result of an {@code UnlockConnector}
 * attempt. Distinct from v1.6 {@code UnlockStatus} which has three values:
 * v2.0.1 adds {@link #ONGOING_AUTHORIZED_TRANSACTION} (requirement F05.FR.02
 * forbids unlocking while an authorised transaction is running) and
 * {@link #UNKNOWN_CONNECTOR}.
 */
public enum UnlockStatus {
    UNLOCKED,
    UNLOCK_FAILED,
    ONGOING_AUTHORIZED_TRANSACTION,
    UNKNOWN_CONNECTOR
}
