package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.security.InstallCertificateUse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstallCertificateUseCaseV201Test {

    private static final TenantId TENANT = new TenantId("demo");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("X");

    @Test
    void rejects_blank_pem() {
        InstallCertificateUseCaseV201 useCase = new InstallCertificateUseCaseV201((t, s, a, p) ->
                null);
        assertThatThrownBy(() -> useCase.installCertificate(TENANT, STATION,
                InstallCertificateUse.CSMS_ROOT_CERTIFICATE, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_pem_over_5500() {
        InstallCertificateUseCaseV201 useCase = new InstallCertificateUseCaseV201((t, s, a, p) ->
                null);
        String tooLong = "a".repeat(5501);
        assertThatThrownBy(() -> useCase.installCertificate(TENANT, STATION,
                InstallCertificateUse.CSMS_ROOT_CERTIFICATE, tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5500");
    }
}
