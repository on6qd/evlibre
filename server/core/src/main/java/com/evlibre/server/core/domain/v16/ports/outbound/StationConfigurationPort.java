package com.evlibre.server.core.domain.v16.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.v16.model.StationConfigurationKey;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.util.List;
import java.util.Optional;

public interface StationConfigurationPort {

    /**
     * Replace the entire configuration for a station (used after GetConfiguration).
     */
    void saveConfiguration(TenantId tenantId, ChargePointIdentity stationIdentity,
                           List<StationConfigurationKey> keys);

    /**
     * Update (or insert) a single configuration key. Key comparison is case-insensitive
     * per OCPP 1.6 configuration-key semantics.
     */
    void updateConfigurationKey(TenantId tenantId, ChargePointIdentity stationIdentity,
                                 StationConfigurationKey key);

    Optional<List<StationConfigurationKey>> getConfiguration(TenantId tenantId,
                                                              ChargePointIdentity stationIdentity);
}
