package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.Get15118EVCertificateResult;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleGet15118EVCertificatePort;
import com.evlibre.server.core.domain.v201.security.CertificateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Relays the ISO 15118 EV certificate EXI payload to a caller-supplied
 * {@link ExiProcessor}. The processor wraps whatever the CSMS knows how to
 * do — forward to the Mobility Operator, the Contract Provisioning Service,
 * or a local signing service. This keeps the core free of ISO 15118 /
 * contract-cert plumbing.
 */
public class HandleGet15118EVCertificateUseCaseV201 implements HandleGet15118EVCertificatePort {

    @FunctionalInterface
    public interface ExiProcessor {
        Get15118EVCertificateResult process(
                TenantId tenantId,
                ChargePointIdentity stationIdentity,
                String iso15118SchemaVersion,
                CertificateAction action,
                String exiRequest);
    }

    private static final Logger log = LoggerFactory.getLogger(HandleGet15118EVCertificateUseCaseV201.class);

    private final ExiProcessor processor;

    public HandleGet15118EVCertificateUseCaseV201(ExiProcessor processor) {
        this.processor = Objects.requireNonNull(processor, "processor");
    }

    @Override
    public Get15118EVCertificateResult handleGet15118EVCertificate(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            String iso15118SchemaVersion,
            CertificateAction action,
            String exiRequest) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(iso15118SchemaVersion, "iso15118SchemaVersion");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(exiRequest, "exiRequest");
        if (iso15118SchemaVersion.isBlank()) {
            throw new IllegalArgumentException("iso15118SchemaVersion must not be blank");
        }
        if (iso15118SchemaVersion.length() > 50) {
            throw new IllegalArgumentException(
                    "iso15118SchemaVersion exceeds 50 char limit (" + iso15118SchemaVersion.length() + ")");
        }
        if (exiRequest.isBlank()) {
            throw new IllegalArgumentException("exiRequest must not be blank");
        }
        if (exiRequest.length() > 5600) {
            throw new IllegalArgumentException(
                    "exiRequest exceeds 5600 char limit (" + exiRequest.length() + ")");
        }

        log.info("Get15118EVCertificate from {} (schema={}, action={}, exiLength={})",
                stationIdentity.value(), iso15118SchemaVersion, action, exiRequest.length());

        return processor.process(tenantId, stationIdentity,
                iso15118SchemaVersion, action, exiRequest);
    }
}
