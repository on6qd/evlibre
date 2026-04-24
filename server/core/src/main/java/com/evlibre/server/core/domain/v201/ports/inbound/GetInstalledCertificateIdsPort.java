package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GetInstalledCertificateIdsResult;
import com.evlibre.server.core.domain.v201.security.GetCertificateIdUse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Inbound port for the CSMS-initiated {@code GetInstalledCertificateIds} use
 * case (block M03). {@code certificateType} is optional on the wire; an
 * {@code null} or empty list asks the station to return all installed
 * certificates regardless of type.
 */
public interface GetInstalledCertificateIdsPort {

    CompletableFuture<GetInstalledCertificateIdsResult> getInstalledCertificateIds(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            List<GetCertificateIdUse> certificateType);
}
