package com.evlibre.server.core.domain.v201.diagnostics;

/**
 * OCPP 2.0.1 {@code UploadLogStatusEnumType} — every state the station can
 * report through a {@code LogStatusNotificationRequest} (block N01).
 *
 * <p>{@link #ACCEPTED_CANCELED} appears when the station receives a new
 * {@code GetLog} mid-upload and aborts the in-flight upload to honour the new
 * one (N01.FR.12). {@link #IDLE} is the default state when there is no
 * upload in progress and may be sent in response to a {@code TriggerMessage}.
 */
public enum UploadLogStatus {
    BAD_MESSAGE,
    IDLE,
    NOT_SUPPORTED_OPERATION,
    PERMISSION_DENIED,
    UPLOADED,
    UPLOAD_FAILURE,
    UPLOADING,
    ACCEPTED_CANCELED
}
