package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.ClearChargingProfileResult;
import com.evlibre.server.core.domain.v201.smartcharging.ClearChargingProfileCriterion;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code ClearChargingProfile} for OCPP 2.0.1 (use case K10).
 * Removes one or more charging profiles from the station either by id, or by
 * AND-combined criterion (purpose/stackLevel/evseId), or both. Per K10.FR.02 at
 * least one of the two must be supplied; passing a blank id and an empty
 * criterion raises {@link IllegalArgumentException}.
 */
public interface ClearChargingProfilePort {

    CompletableFuture<ClearChargingProfileResult> clearChargingProfile(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            Integer chargingProfileId,
            ClearChargingProfileCriterion criterion);
}
