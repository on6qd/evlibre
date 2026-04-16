package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface RemoteStartTransactionPort {

    CompletableFuture<CommandResult> remoteStart(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                  String idTag, Integer connectorId);

    /**
     * Variant that includes a chargingProfile. Per OCPP 1.6 §5.15 the profile's
     * chargingProfilePurpose MUST be TxProfile; the implementation validates this.
     */
    CompletableFuture<CommandResult> remoteStart(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                  String idTag, Integer connectorId,
                                                  Map<String, Object> chargingProfile);
}
