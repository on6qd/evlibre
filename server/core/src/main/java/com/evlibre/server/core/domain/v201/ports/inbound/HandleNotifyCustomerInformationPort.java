package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;

/**
 * Accepts a single frame of an OCPP 2.0.1 {@code NotifyCustomerInformation}.
 * The implementation is responsible for concatenating the per-frame
 * {@code data} chunks under the same {@code requestId} and delivering the
 * assembled string to a sink when the station marks the last frame with
 * {@code tbc=false}.
 */
public interface HandleNotifyCustomerInformationPort {

    void handleFrame(TenantId tenantId,
                     ChargePointIdentity stationIdentity,
                     int requestId,
                     int seqNo,
                     boolean tbc,
                     String data);
}
