package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.InstallCertificateResult;
import com.evlibre.server.core.domain.v201.security.InstallCertificateUse;

import java.util.concurrent.CompletableFuture;

/**
 * Inbound port for the CSMS-initiated {@code InstallCertificate} use case
 * (block M01). Installs a CA root certificate on the station's trust store.
 * Station MUST verify the certificate before accepting.
 */
public interface InstallCertificatePort {

    CompletableFuture<InstallCertificateResult> installCertificate(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            InstallCertificateUse certificateType,
            String certificate);
}
