package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.DeleteCertificateResult;
import com.evlibre.server.core.domain.v201.security.CertificateHashData;

import java.util.concurrent.CompletableFuture;

/**
 * Inbound port for the CSMS-initiated {@code DeleteCertificate} use case
 * (block M04). The certificate to remove is identified by the full hash
 * tuple previously returned by {@code GetInstalledCertificateIds}.
 */
public interface DeleteCertificatePort {

    CompletableFuture<DeleteCertificateResult> deleteCertificate(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            CertificateHashData certificateHashData);
}
