package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.displaymessage.ClearDisplayMessageResult;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code ClearDisplayMessage} (OCPP 2.0.1 O03 — Clear
 * Display Message). Removes a single installed display message from the
 * station by id.
 */
public interface ClearDisplayMessagePort {

    CompletableFuture<ClearDisplayMessageResult> clearDisplayMessage(TenantId tenantId,
                                                                       ChargePointIdentity stationIdentity,
                                                                       int id);
}
