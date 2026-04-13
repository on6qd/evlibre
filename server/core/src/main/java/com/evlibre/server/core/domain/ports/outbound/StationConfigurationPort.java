package com.evlibre.server.core.domain.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.StationConfigurationKey;
import com.evlibre.server.core.domain.model.TenantId;

import java.util.List;
import java.util.Optional;

public interface StationConfigurationPort {

    void saveConfiguration(TenantId tenantId, ChargePointIdentity stationIdentity,
                           List<StationConfigurationKey> keys);

    Optional<List<StationConfigurationKey>> getConfiguration(TenantId tenantId,
                                                              ChargePointIdentity stationIdentity);
}
