package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.ClearCacheResult;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code ClearCache} for OCPP 2.0.1 (use case C11).
 *
 * <p>Requests the Charging Station to clear every IdToken currently held in
 * its Authorization Cache. Logically distinct from the Local Authorization
 * List — clearing the cache does not touch that list.
 *
 * <p>The request carries no arguments beyond the session identifiers; all
 * divergence between v1.6 and v2.0.1 is in the response shape, so this port
 * is kept intentionally separate from its v1.6 sibling.
 */
public interface ClearCachePort {

    CompletableFuture<ClearCacheResult> clearCache(
            TenantId tenantId,
            ChargePointIdentity stationIdentity);
}
