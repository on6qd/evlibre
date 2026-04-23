package com.evlibre.server.core.domain.v201.firmware;

/**
 * Wire-form codec for OCPP 2.0.1 firmware-related enums. Centralised so
 * inbound handlers and outbound use cases agree on the spec spellings —
 * mirrors the role {@code ChargingProfileWire} plays for smart charging.
 */
public final class FirmwareWire {

    private FirmwareWire() {}

    public static String statusToWire(FirmwareStatus status) {
        return switch (status) {
            case DOWNLOADED -> "Downloaded";
            case DOWNLOAD_FAILED -> "DownloadFailed";
            case DOWNLOADING -> "Downloading";
            case DOWNLOAD_PAUSED -> "DownloadPaused";
            case DOWNLOAD_SCHEDULED -> "DownloadScheduled";
            case IDLE -> "Idle";
            case INSTALLATION_FAILED -> "InstallationFailed";
            case INSTALLED -> "Installed";
            case INSTALLING -> "Installing";
            case INSTALL_REBOOTING -> "InstallRebooting";
            case INSTALL_SCHEDULED -> "InstallScheduled";
            case INSTALL_VERIFICATION_FAILED -> "InstallVerificationFailed";
            case INVALID_SIGNATURE -> "InvalidSignature";
            case SIGNATURE_VERIFIED -> "SignatureVerified";
        };
    }

    public static PublishFirmwareStatus publishStatusFromWire(String wire) {
        return switch (wire) {
            case "Idle" -> PublishFirmwareStatus.IDLE;
            case "DownloadScheduled" -> PublishFirmwareStatus.DOWNLOAD_SCHEDULED;
            case "Downloading" -> PublishFirmwareStatus.DOWNLOADING;
            case "Downloaded" -> PublishFirmwareStatus.DOWNLOADED;
            case "DownloadPaused" -> PublishFirmwareStatus.DOWNLOAD_PAUSED;
            case "DownloadFailed" -> PublishFirmwareStatus.DOWNLOAD_FAILED;
            case "ChecksumVerified" -> PublishFirmwareStatus.CHECKSUM_VERIFIED;
            case "InvalidChecksum" -> PublishFirmwareStatus.INVALID_CHECKSUM;
            case "Published" -> PublishFirmwareStatus.PUBLISHED;
            case "PublishFailed" -> PublishFirmwareStatus.PUBLISH_FAILED;
            default -> throw new IllegalArgumentException(
                    "Unknown PublishFirmwareStatus wire value: " + wire);
        };
    }

    public static FirmwareStatus statusFromWire(String wire) {
        return switch (wire) {
            case "Downloaded" -> FirmwareStatus.DOWNLOADED;
            case "DownloadFailed" -> FirmwareStatus.DOWNLOAD_FAILED;
            case "Downloading" -> FirmwareStatus.DOWNLOADING;
            case "DownloadPaused" -> FirmwareStatus.DOWNLOAD_PAUSED;
            case "DownloadScheduled" -> FirmwareStatus.DOWNLOAD_SCHEDULED;
            case "Idle" -> FirmwareStatus.IDLE;
            case "InstallationFailed" -> FirmwareStatus.INSTALLATION_FAILED;
            case "Installed" -> FirmwareStatus.INSTALLED;
            case "Installing" -> FirmwareStatus.INSTALLING;
            case "InstallRebooting" -> FirmwareStatus.INSTALL_REBOOTING;
            case "InstallScheduled" -> FirmwareStatus.INSTALL_SCHEDULED;
            case "InstallVerificationFailed" -> FirmwareStatus.INSTALL_VERIFICATION_FAILED;
            case "InvalidSignature" -> FirmwareStatus.INVALID_SIGNATURE;
            case "SignatureVerified" -> FirmwareStatus.SIGNATURE_VERIFIED;
            default -> throw new IllegalArgumentException("Unknown FirmwareStatus wire value: " + wire);
        };
    }
}
