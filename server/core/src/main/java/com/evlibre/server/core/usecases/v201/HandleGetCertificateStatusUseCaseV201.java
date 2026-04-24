package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GetCertificateStatusResult;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleGetCertificateStatusPort;
import com.evlibre.server.core.domain.v201.security.OcspRequestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Relays the station's OCSP request to a caller-supplied
 * {@link OcspResolver}. The resolver is the outbound boundary — it may use
 * an HTTPS-backed OCSP responder client, a pre-fetched cache, or reject the
 * request outright. This keeps the core pure and lets each CSMS deployment
 * bring its own OCSP plumbing.
 */
public class HandleGetCertificateStatusUseCaseV201 implements HandleGetCertificateStatusPort {

    @FunctionalInterface
    public interface OcspResolver {
        GetCertificateStatusResult resolve(
                TenantId tenantId,
                ChargePointIdentity stationIdentity,
                OcspRequestData ocspRequestData);
    }

    private static final Logger log = LoggerFactory.getLogger(HandleGetCertificateStatusUseCaseV201.class);

    private final OcspResolver resolver;

    public HandleGetCertificateStatusUseCaseV201(OcspResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    @Override
    public GetCertificateStatusResult handleGetCertificateStatus(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            OcspRequestData ocspRequestData) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(ocspRequestData, "ocspRequestData");
        log.info("GetCertificateStatus from {} (serial={}, responder={})",
                stationIdentity.value(),
                ocspRequestData.serialNumber(),
                ocspRequestData.responderURL());
        return resolver.resolve(tenantId, stationIdentity, ocspRequestData);
    }
}
