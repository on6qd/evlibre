package com.evlibre.server.core.domain.v201.diagnostics;

/**
 * Wire-form codec for OCPP 2.0.1 diagnostics-related enums (block N).
 * Centralised so inbound handlers and outbound use cases agree on the spec
 * spellings — mirrors the role {@code FirmwareWire} plays for block L.
 */
public final class DiagnosticsWire {

    private DiagnosticsWire() {}

    public static String uploadLogStatusToWire(UploadLogStatus status) {
        return switch (status) {
            case BAD_MESSAGE -> "BadMessage";
            case IDLE -> "Idle";
            case NOT_SUPPORTED_OPERATION -> "NotSupportedOperation";
            case PERMISSION_DENIED -> "PermissionDenied";
            case UPLOADED -> "Uploaded";
            case UPLOAD_FAILURE -> "UploadFailure";
            case UPLOADING -> "Uploading";
            case ACCEPTED_CANCELED -> "AcceptedCanceled";
        };
    }

    public static UploadLogStatus uploadLogStatusFromWire(String wire) {
        return switch (wire) {
            case "BadMessage" -> UploadLogStatus.BAD_MESSAGE;
            case "Idle" -> UploadLogStatus.IDLE;
            case "NotSupportedOperation" -> UploadLogStatus.NOT_SUPPORTED_OPERATION;
            case "PermissionDenied" -> UploadLogStatus.PERMISSION_DENIED;
            case "Uploaded" -> UploadLogStatus.UPLOADED;
            case "UploadFailure" -> UploadLogStatus.UPLOAD_FAILURE;
            case "Uploading" -> UploadLogStatus.UPLOADING;
            case "AcceptedCanceled" -> UploadLogStatus.ACCEPTED_CANCELED;
            default -> throw new IllegalArgumentException(
                    "Unknown UploadLogStatus wire value: " + wire);
        };
    }
}
