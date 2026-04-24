package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.SignCertificateResult;
import com.evlibre.server.core.domain.v201.security.CertificateSigningUse;
import com.evlibre.server.core.usecases.v201.HandleSignCertificateUseCaseV201;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fake SignCertificate policy: captures every CSR submission and replies with
 * a configurable result (default Accepted). Tests can flip the next result via
 * {@link #setNextResult(SignCertificateResult)} to exercise the reject path.
 */
public class FakeSignCertificatePolicy implements HandleSignCertificateUseCaseV201.Policy {

    public record Submission(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            String csr,
            CertificateSigningUse certificateType) {}

    private final List<Submission> submissions = Collections.synchronizedList(new ArrayList<>());
    private volatile SignCertificateResult nextResult = SignCertificateResult.accepted();

    public void setNextResult(SignCertificateResult result) {
        this.nextResult = result;
    }

    @Override
    public SignCertificateResult decide(TenantId tenantId,
                                        ChargePointIdentity stationIdentity,
                                        String csr,
                                        CertificateSigningUse certificateType) {
        submissions.add(new Submission(tenantId, stationIdentity, csr, certificateType));
        return nextResult;
    }

    public List<Submission> submissions() {
        return List.copyOf(submissions);
    }

    public void clear() {
        submissions.clear();
        nextResult = SignCertificateResult.accepted();
    }
}
