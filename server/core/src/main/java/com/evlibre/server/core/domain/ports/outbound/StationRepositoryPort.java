package com.evlibre.server.core.domain.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.ChargingStation;
import com.evlibre.server.core.domain.model.TenantId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StationRepositoryPort {

    void save(ChargingStation station);

    Optional<ChargingStation> findById(UUID id);

    Optional<ChargingStation> findByTenantAndIdentity(TenantId tenantId, ChargePointIdentity identity);

    List<ChargingStation> findByTenant(TenantId tenantId);
}
