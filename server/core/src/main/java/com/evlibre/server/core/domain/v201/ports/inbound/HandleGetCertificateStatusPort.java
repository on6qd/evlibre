package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GetCertificateStatusResult;
import com.evlibre.server.core.domain.v201.security.OcspRequestData;

/**
 * Inbound port for {@code GetCertificateStatusRequest} (block A04). Station
 * asks the CSMS to relay an OCSP query to the issuing CA and return the
 * signed OCSP response. Required for ISO 15118 Plug &amp; Charge so stations
 * can verify EV contract certificates without direct network access to the
 * CA's OCSP responder.
 */
public interface HandleGetCertificateStatusPort {

    GetCertificateStatusResult handleGetCertificateStatus(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            OcspRequestData ocspRequestData);
}
