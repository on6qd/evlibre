package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code RequestStartStopStatusEnumType} — the station's verdict on
 * a CSMS-initiated {@code RequestStartTransaction} or
 * {@code RequestStopTransaction} command.
 *
 * <p>{@code Accepted} means the station will honour the request; it does NOT
 * guarantee that the transaction actually started/stopped — the station will
 * confirm that separately via a {@code TransactionEvent} with the matching
 * {@code remoteStartId}/{@code transactionId}.
 */
public enum RequestStartStopStatus {
    ACCEPTED,
    REJECTED
}
