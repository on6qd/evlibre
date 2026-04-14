package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.StationConfigurationKey;
import com.evlibre.server.core.domain.model.TenantId;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GetConfigurationPort {

    CompletableFuture<List<StationConfigurationKey>> getConfiguration(TenantId tenantId,
                                                                       ChargePointIdentity stationIdentity,
                                                                       List<String> keys);
}
