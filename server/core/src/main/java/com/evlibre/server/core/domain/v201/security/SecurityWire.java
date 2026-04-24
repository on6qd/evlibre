package com.evlibre.server.core.domain.v201.security;

/**
 * Wire-format helpers for the OCPP 2.0.1 security enum types. Mirrors the
 * {@code DiagnosticsWire} / {@code FirmwareWire} pattern so enum parsing lives
 * in one place.
 */
public final class SecurityWire {

    private SecurityWire() {}

    public static String certificateSigningUseToWire(CertificateSigningUse use) {
        return switch (use) {
            case CHARGING_STATION_CERTIFICATE -> "ChargingStationCertificate";
            case V2G_CERTIFICATE -> "V2GCertificate";
        };
    }

    public static CertificateSigningUse certificateSigningUseFromWire(String wire) {
        return switch (wire) {
            case "ChargingStationCertificate" -> CertificateSigningUse.CHARGING_STATION_CERTIFICATE;
            case "V2GCertificate" -> CertificateSigningUse.V2G_CERTIFICATE;
            default -> throw new IllegalArgumentException(
                    "Unknown CertificateSigningUseEnumType: " + wire);
        };
    }
}
