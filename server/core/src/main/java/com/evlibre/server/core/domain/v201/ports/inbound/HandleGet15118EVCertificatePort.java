package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.Get15118EVCertificateResult;
import com.evlibre.server.core.domain.v201.security.CertificateAction;

/**
 * Inbound port for {@code Get15118EVCertificateRequest} (block M06). The
 * station forwards an EXI-encoded ISO 15118 Certificate{Installation|Update}
 * request from the EV and expects the CSMS to return the matching response
 * produced by the contract-certificate issuer / Mobility Operator.
 */
public interface HandleGet15118EVCertificatePort {

    Get15118EVCertificateResult handleGet15118EVCertificate(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            String iso15118SchemaVersion,
            CertificateAction action,
            String exiRequest);
}
