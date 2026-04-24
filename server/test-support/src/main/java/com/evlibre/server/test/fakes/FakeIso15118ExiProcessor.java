package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.Get15118EVCertificateResult;
import com.evlibre.server.core.domain.v201.security.CertificateAction;
import com.evlibre.server.core.usecases.v201.HandleGet15118EVCertificateUseCaseV201;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fake EXI processor for {@code Get15118EVCertificate} tests. Captures every
 * forwarded EV certificate request and returns a configurable result (default
 * Accepted with a placeholder EXI response). Real deployments plug in an EXI
 * codec and a Mobility-Operator / CPS client here.
 */
public class FakeIso15118ExiProcessor
        implements HandleGet15118EVCertificateUseCaseV201.ExiProcessor {

    public record Request(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            String iso15118SchemaVersion,
            CertificateAction action,
            String exiRequest) {}

    private final List<Request> requests = Collections.synchronizedList(new ArrayList<>());
    private volatile Get15118EVCertificateResult nextResult =
            Get15118EVCertificateResult.accepted("exi-response-placeholder-base64");

    public void setNextResult(Get15118EVCertificateResult result) {
        this.nextResult = result;
    }

    @Override
    public Get15118EVCertificateResult process(TenantId tenantId,
                                               ChargePointIdentity stationIdentity,
                                               String iso15118SchemaVersion,
                                               CertificateAction action,
                                               String exiRequest) {
        requests.add(new Request(tenantId, stationIdentity,
                iso15118SchemaVersion, action, exiRequest));
        return nextResult;
    }

    public List<Request> requests() {
        return List.copyOf(requests);
    }

    public void clear() {
        requests.clear();
        nextResult = Get15118EVCertificateResult.accepted("exi-response-placeholder-base64");
    }
}
