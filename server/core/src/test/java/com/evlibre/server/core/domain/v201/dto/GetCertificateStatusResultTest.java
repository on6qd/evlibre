package com.evlibre.server.core.domain.v201.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetCertificateStatusResultTest {

    @Test
    void accepted_requires_non_null_ocsp_result() {
        assertThatThrownBy(() -> GetCertificateStatusResult.accepted(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void accepted_requires_non_blank_ocsp_result() {
        assertThatThrownBy(() -> GetCertificateStatusResult.accepted("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void accepted_with_valid_ocsp_result_is_allowed() {
        GetCertificateStatusResult r = GetCertificateStatusResult.accepted("MIIBxQoBAKCCAb4");
        assertThat(r.isAccepted()).isTrue();
        assertThat(r.ocspResult()).isEqualTo("MIIBxQoBAKCCAb4");
    }

    @Test
    void failed_allows_absent_ocsp_result() {
        GetCertificateStatusResult r = GetCertificateStatusResult.failed("OcspUnreachable");
        assertThat(r.isAccepted()).isFalse();
        assertThat(r.ocspResult()).isNull();
        assertThat(r.statusInfoReason()).isEqualTo("OcspUnreachable");
    }

    @Test
    void rejects_ocsp_result_over_5500_chars() {
        String tooLong = "a".repeat(5501);
        assertThatThrownBy(() -> GetCertificateStatusResult.accepted(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5500");
    }
}
