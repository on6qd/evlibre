package com.evlibre.server.core.domain.v201.firmware;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FirmwareTest {

    private static final Instant RETRIEVE = Instant.parse("2027-02-01T03:00:00Z");
    private static final Instant INSTALL = Instant.parse("2027-02-01T04:00:00Z");

    @Test
    void basic_accepts_minimal_fields() {
        var fw = Firmware.basic("https://csms/fw.bin", RETRIEVE);

        assertThat(fw.location()).isEqualTo("https://csms/fw.bin");
        assertThat(fw.retrieveDateTime()).isEqualTo(RETRIEVE);
        assertThat(fw.installDateTime()).isNull();
        assertThat(fw.signingCertificate()).isNull();
        assertThat(fw.signature()).isNull();
    }

    @Test
    void accepts_certificate_and_signature_together() {
        var fw = new Firmware(
                "https://csms/fw.bin", RETRIEVE, INSTALL,
                "-----BEGIN CERTIFICATE-----\nMIIB...\n-----END CERTIFICATE-----",
                "c2lnbmF0dXJlLWJ5dGVz");

        assertThat(fw.signingCertificate()).startsWith("-----BEGIN");
        assertThat(fw.signature()).isEqualTo("c2lnbmF0dXJlLWJ5dGVz");
    }

    @Test
    void rejects_certificate_without_signature() {
        // L01.FR.11 + L01.FR.12: the station needs both to verify the firmware.
        assertThatThrownBy(() -> new Firmware(
                "https://csms/fw.bin", RETRIEVE, null,
                "-----BEGIN CERTIFICATE-----\nMIIB...\n-----END CERTIFICATE-----",
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signingCertificate and signature must be supplied together");
    }

    @Test
    void rejects_signature_without_certificate() {
        assertThatThrownBy(() -> new Firmware(
                "https://csms/fw.bin", RETRIEVE, null,
                null,
                "c2lnbmF0dXJlLWJ5dGVz"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signingCertificate and signature must be supplied together");
    }

    @Test
    void rejects_blank_location() {
        assertThatThrownBy(() -> new Firmware("", RETRIEVE, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("location");
    }

    @Test
    void rejects_location_over_512_chars() {
        String longLoc = "https://csms/" + "x".repeat(500);
        assertThatThrownBy(() -> new Firmware(longLoc, RETRIEVE, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("512");
    }

    @Test
    void rejects_signingCertificate_over_5500_chars() {
        String longCert = "x".repeat(5501);
        assertThatThrownBy(() -> new Firmware(
                "https://csms/fw.bin", RETRIEVE, null, longCert, "sig"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5500");
    }

    @Test
    void rejects_signature_over_800_chars() {
        String longSig = "x".repeat(801);
        assertThatThrownBy(() -> new Firmware(
                "https://csms/fw.bin", RETRIEVE, null, "cert", longSig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("800");
    }
}
