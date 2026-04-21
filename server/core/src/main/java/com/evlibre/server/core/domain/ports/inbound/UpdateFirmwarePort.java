package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.util.concurrent.CompletableFuture;

public interface UpdateFirmwarePort {

    CompletableFuture<Void> updateFirmware(TenantId tenantId, ChargePointIdentity stationIdentity,
                                            String location, String retrieveDate,
                                            Integer retries, Integer retryInterval);
}
