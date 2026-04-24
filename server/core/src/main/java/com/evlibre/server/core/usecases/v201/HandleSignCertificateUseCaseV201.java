package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.SignCertificateResult;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleSignCertificatePort;
import com.evlibre.server.core.domain.v201.security.CertificateSigningUse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Default inbound use case for {@code SignCertificate}. Validates the CSR
 * length per the spec (maxLength 5500) and delegates the Accept/Reject
 * decision to a caller-supplied {@link Policy} — different CSMS deployments
 * will want different gating (e.g. only accept CSRs whose CommonName matches
 * the station identity, or rate-limit per station).
 */
public class HandleSignCertificateUseCaseV201 implements HandleSignCertificatePort {

    @FunctionalInterface
    public interface Policy {
        SignCertificateResult decide(
                TenantId tenantId,
                ChargePointIdentity stationIdentity,
                String csr,
                CertificateSigningUse certificateType);
    }

    private static final Logger log = LoggerFactory.getLogger(HandleSignCertificateUseCaseV201.class);

    private final Policy policy;

    public HandleSignCertificateUseCaseV201(Policy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    public SignCertificateResult handleSignCertificate(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            String csr,
            CertificateSigningUse certificateType) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(csr, "csr");
        if (csr.isBlank()) {
            throw new IllegalArgumentException("csr must not be blank");
        }
        if (csr.length() > 5500) {
            throw new IllegalArgumentException(
                    "csr exceeds 5500 char limit (" + csr.length() + ")");
        }

        CertificateSigningUse effectiveType = certificateType != null
                ? certificateType
                : CertificateSigningUse.CHARGING_STATION_CERTIFICATE;

        log.info("SignCertificate from {} (certificateType={}, csrLength={})",
                stationIdentity.value(), effectiveType, csr.length());

        return policy.decide(tenantId, stationIdentity, csr, effectiveType);
    }
}
