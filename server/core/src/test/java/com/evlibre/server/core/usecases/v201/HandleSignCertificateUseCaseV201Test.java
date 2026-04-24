package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GenericStatus;
import com.evlibre.server.core.domain.v201.dto.SignCertificateResult;
import com.evlibre.server.core.domain.v201.security.CertificateSigningUse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HandleSignCertificateUseCaseV201Test {

    private static final TenantId TENANT = new TenantId("demo");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("CSR-1");
    private static final String CSR = """
            -----BEGIN CERTIFICATE REQUEST-----
            MIIBYTCByQIBADBgMQswCQYDVQQGEwJCRTENMAsGA1UECgwEYWNtZTEVMBMGA1UE
            -----END CERTIFICATE REQUEST-----""";

    @Test
    void defaults_certificate_type_to_charging_station_when_absent() {
        AtomicReference<CertificateSigningUse> seen = new AtomicReference<>();
        HandleSignCertificateUseCaseV201 useCase = new HandleSignCertificateUseCaseV201(
                (t, s, csr, type) -> {
                    seen.set(type);
                    return SignCertificateResult.accepted();
                });

        SignCertificateResult result = useCase.handleSignCertificate(TENANT, STATION, CSR, null);

        assertThat(seen.get()).isEqualTo(CertificateSigningUse.CHARGING_STATION_CERTIFICATE);
        assertThat(result.status()).isEqualTo(GenericStatus.ACCEPTED);
    }

    @Test
    void passes_explicit_v2g_certificate_type_to_policy() {
        AtomicReference<CertificateSigningUse> seen = new AtomicReference<>();
        HandleSignCertificateUseCaseV201 useCase = new HandleSignCertificateUseCaseV201(
                (t, s, csr, type) -> {
                    seen.set(type);
                    return SignCertificateResult.accepted();
                });

        useCase.handleSignCertificate(TENANT, STATION, CSR,
                CertificateSigningUse.V2G_CERTIFICATE);

        assertThat(seen.get()).isEqualTo(CertificateSigningUse.V2G_CERTIFICATE);
    }

    @Test
    void surfaces_rejection_reason_from_policy() {
        HandleSignCertificateUseCaseV201 useCase = new HandleSignCertificateUseCaseV201(
                (t, s, csr, type) -> SignCertificateResult.rejected("InvalidCsr"));

        SignCertificateResult result = useCase.handleSignCertificate(TENANT, STATION, CSR, null);

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.statusInfoReason()).isEqualTo("InvalidCsr");
    }

    @Test
    void rejects_blank_csr() {
        HandleSignCertificateUseCaseV201 useCase = new HandleSignCertificateUseCaseV201(
                (t, s, csr, type) -> SignCertificateResult.accepted());
        assertThatThrownBy(() ->
                useCase.handleSignCertificate(TENANT, STATION, "   ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_csr_over_5500_chars() {
        HandleSignCertificateUseCaseV201 useCase = new HandleSignCertificateUseCaseV201(
                (t, s, csr, type) -> SignCertificateResult.accepted());
        String tooLong = "a".repeat(5501);
        assertThatThrownBy(() ->
                useCase.handleSignCertificate(TENANT, STATION, tooLong, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5500");
    }
}
