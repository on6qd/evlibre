package com.evlibre.server.core.domain.v201.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertificateHashDataTest {

    @Test
    void accepts_valid_tuple() {
        CertificateHashData d = new CertificateHashData(
                HashAlgorithm.SHA256, "name-hash", "key-hash", "serial-0A");
        assertThat(d.hashAlgorithm()).isEqualTo(HashAlgorithm.SHA256);
    }

    @Test
    void rejects_issuer_name_hash_over_128() {
        assertThatThrownBy(() -> new CertificateHashData(
                HashAlgorithm.SHA256, "a".repeat(129), "k", "s"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("128");
    }

    @Test
    void rejects_issuer_key_hash_over_128() {
        assertThatThrownBy(() -> new CertificateHashData(
                HashAlgorithm.SHA256, "n", "a".repeat(129), "s"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("128");
    }

    @Test
    void rejects_serial_over_40() {
        assertThatThrownBy(() -> new CertificateHashData(
                HashAlgorithm.SHA256, "n", "k", "a".repeat(41)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("40");
    }

    @Test
    void chain_defaults_children_to_empty() {
        CertificateHashData d = new CertificateHashData(
                HashAlgorithm.SHA256, "n", "k", "s");
        CertificateHashDataChain c = CertificateHashDataChain.of(
                GetCertificateIdUse.CSMS_ROOT_CERTIFICATE, d);
        assertThat(c.childCertificateHashData()).isEmpty();
    }
}
