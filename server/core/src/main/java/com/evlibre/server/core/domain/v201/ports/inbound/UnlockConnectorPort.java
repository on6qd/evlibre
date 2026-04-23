package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.UnlockConnectorResult;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code UnlockConnector} for OCPP 2.0.1 (use case F05).
 *
 * <p>Asks the station to release the cable retention lock on a specific
 * connector. Unlike v1.6 which took a single {@code connectorId} integer,
 * v2.0.1 addresses the connector via the {@code (evseId, connectorId)} pair.
 *
 * <p>Both {@code evseId} and {@code connectorId} are required and SHALL be
 * {@code > 0} per spec.
 */
public interface UnlockConnectorPort {

    CompletableFuture<UnlockConnectorResult> unlockConnector(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int evseId,
            int connectorId);
}
