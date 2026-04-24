package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.SignCertificateResult;
import com.evlibre.server.core.domain.v201.security.CertificateSigningUse;

/**
 * Inbound port for {@code SignCertificateRequest} (block A02). The station
 * submits a PEM-encoded CSR; the CSMS acknowledges custody of the CSR
 * synchronously (Accepted / Rejected + optional reasonCode). The actual
 * signed certificate is delivered asynchronously later via an outbound
 * {@code CertificateSigned} command (not in phase 7 scope).
 *
 * <p>{@code certificateType} is optional on the wire; when omitted the spec
 * defaults to {@link CertificateSigningUse#CHARGING_STATION_CERTIFICATE}.
 */
public interface HandleSignCertificatePort {

    SignCertificateResult handleSignCertificate(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            String csr,
            CertificateSigningUse certificateType);
}
