package com.evlibre.server.core.domain.v201.security;

import java.util.LinkedHashMap;
import java.util.Map;

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

    public static String installCertificateUseToWire(InstallCertificateUse use) {
        return switch (use) {
            case V2G_ROOT_CERTIFICATE -> "V2GRootCertificate";
            case MO_ROOT_CERTIFICATE -> "MORootCertificate";
            case CSMS_ROOT_CERTIFICATE -> "CSMSRootCertificate";
            case MANUFACTURER_ROOT_CERTIFICATE -> "ManufacturerRootCertificate";
        };
    }

    public static InstallCertificateUse installCertificateUseFromWire(String wire) {
        return switch (wire) {
            case "V2GRootCertificate" -> InstallCertificateUse.V2G_ROOT_CERTIFICATE;
            case "MORootCertificate" -> InstallCertificateUse.MO_ROOT_CERTIFICATE;
            case "CSMSRootCertificate" -> InstallCertificateUse.CSMS_ROOT_CERTIFICATE;
            case "ManufacturerRootCertificate" -> InstallCertificateUse.MANUFACTURER_ROOT_CERTIFICATE;
            default -> throw new IllegalArgumentException(
                    "Unknown InstallCertificateUseEnumType: " + wire);
        };
    }

    public static String getCertificateIdUseToWire(GetCertificateIdUse use) {
        return switch (use) {
            case V2G_ROOT_CERTIFICATE -> "V2GRootCertificate";
            case MO_ROOT_CERTIFICATE -> "MORootCertificate";
            case CSMS_ROOT_CERTIFICATE -> "CSMSRootCertificate";
            case V2G_CERTIFICATE_CHAIN -> "V2GCertificateChain";
            case MANUFACTURER_ROOT_CERTIFICATE -> "ManufacturerRootCertificate";
        };
    }

    public static GetCertificateIdUse getCertificateIdUseFromWire(String wire) {
        return switch (wire) {
            case "V2GRootCertificate" -> GetCertificateIdUse.V2G_ROOT_CERTIFICATE;
            case "MORootCertificate" -> GetCertificateIdUse.MO_ROOT_CERTIFICATE;
            case "CSMSRootCertificate" -> GetCertificateIdUse.CSMS_ROOT_CERTIFICATE;
            case "V2GCertificateChain" -> GetCertificateIdUse.V2G_CERTIFICATE_CHAIN;
            case "ManufacturerRootCertificate" -> GetCertificateIdUse.MANUFACTURER_ROOT_CERTIFICATE;
            default -> throw new IllegalArgumentException(
                    "Unknown GetCertificateIdUseEnumType: " + wire);
        };
    }

    public static String hashAlgorithmToWire(HashAlgorithm algorithm) {
        return switch (algorithm) {
            case SHA256 -> "SHA256";
            case SHA384 -> "SHA384";
            case SHA512 -> "SHA512";
        };
    }

    public static HashAlgorithm hashAlgorithmFromWire(String wire) {
        return switch (wire) {
            case "SHA256" -> HashAlgorithm.SHA256;
            case "SHA384" -> HashAlgorithm.SHA384;
            case "SHA512" -> HashAlgorithm.SHA512;
            default -> throw new IllegalArgumentException(
                    "Unknown HashAlgorithmEnumType: " + wire);
        };
    }

    public static Map<String, Object> certificateHashDataToWire(CertificateHashData data) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("hashAlgorithm", hashAlgorithmToWire(data.hashAlgorithm()));
        out.put("issuerNameHash", data.issuerNameHash());
        out.put("issuerKeyHash", data.issuerKeyHash());
        out.put("serialNumber", data.serialNumber());
        return out;
    }

    public static CertificateHashData certificateHashDataFromWire(Map<?, ?> m) {
        return new CertificateHashData(
                hashAlgorithmFromWire(String.valueOf(m.get("hashAlgorithm"))),
                String.valueOf(m.get("issuerNameHash")),
                String.valueOf(m.get("issuerKeyHash")),
                String.valueOf(m.get("serialNumber")));
    }
}
