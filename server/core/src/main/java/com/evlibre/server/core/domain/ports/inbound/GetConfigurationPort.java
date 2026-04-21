package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.v16.model.StationConfigurationKey;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GetConfigurationPort {

    CompletableFuture<List<StationConfigurationKey>> getConfiguration(TenantId tenantId,
                                                                       ChargePointIdentity stationIdentity,
                                                                       List<String> keys);
}
