package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GetLocalListVersionResult;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code GetLocalListVersion} for OCPP 2.0.1 (use case D02).
 *
 * <p>Asks the station to report the version number of its Local Authorization
 * List. A returned value of {@code 0} means no list is installed (either the
 * feature is disabled or no {@code SendLocalList} has yet been applied);
 * any value {@code > 0} is the concrete version the CSMS last supplied.
 *
 * <p>The v2.0.1 response field is {@code versionNumber} (differs from v1.6's
 * {@code listVersion}), which is why this port is kept separate from its v1.6
 * sibling.
 */
public interface GetLocalListVersionPort {

    CompletableFuture<GetLocalListVersionResult> getLocalListVersion(
            TenantId tenantId,
            ChargePointIdentity stationIdentity);
}
