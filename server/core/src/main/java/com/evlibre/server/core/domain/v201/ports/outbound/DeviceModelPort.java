package com.evlibre.server.core.domain.v201.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.v201.model.DeviceModelVariable;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.util.List;
import java.util.Optional;

/**
 * Port for storing OCPP 2.0.1 device model data received via NotifyReport.
 */
public interface DeviceModelPort {

    void saveVariables(TenantId tenantId, ChargePointIdentity stationIdentity,
                       List<DeviceModelVariable> variables);

    Optional<List<DeviceModelVariable>> getVariables(TenantId tenantId,
                                                      ChargePointIdentity stationIdentity);
}
