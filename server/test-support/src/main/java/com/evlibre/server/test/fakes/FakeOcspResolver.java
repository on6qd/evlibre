package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GetCertificateStatusResult;
import com.evlibre.server.core.domain.v201.security.OcspRequestData;
import com.evlibre.server.core.usecases.v201.HandleGetCertificateStatusUseCaseV201;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fake OCSP resolver for tests: captures every relay attempt and returns a
 * configurable result (default Accepted with a placeholder OCSP payload).
 */
public class FakeOcspResolver implements HandleGetCertificateStatusUseCaseV201.OcspResolver {

    public record Request(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            OcspRequestData ocspRequestData) {}

    private final List<Request> requests = Collections.synchronizedList(new ArrayList<>());
    private volatile GetCertificateStatusResult nextResult =
            GetCertificateStatusResult.accepted("MIIBxQoBAKCCAb4wggG6BgkrBgEFBQcwAQEEggGrMIIBpzCBkKIWB");

    public void setNextResult(GetCertificateStatusResult result) {
        this.nextResult = result;
    }

    @Override
    public GetCertificateStatusResult resolve(TenantId tenantId,
                                              ChargePointIdentity stationIdentity,
                                              OcspRequestData ocspRequestData) {
        requests.add(new Request(tenantId, stationIdentity, ocspRequestData));
        return nextResult;
    }

    public List<Request> requests() {
        return List.copyOf(requests);
    }

    public void clear() {
        requests.clear();
        nextResult = GetCertificateStatusResult.accepted(
                "MIIBxQoBAKCCAb4wggG6BgkrBgEFBQcwAQEEggGrMIIBpzCBkKIWB");
    }
}
