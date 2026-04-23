package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.SendLocalListResult;
import com.evlibre.server.core.domain.v201.model.AuthorizationData;
import com.evlibre.server.core.domain.v201.model.UpdateType;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code SendLocalList} for OCPP 2.0.1 (use case D01).
 *
 * <p>Synchronises the station's Local Authorization List with the CSMS-held
 * copy. {@code versionNumber} must be {@code > 0} per D01.FR.18 — the value
 * {@code 0} is reserved in {@code GetLocalListVersionResponse} to mean "no
 * list installed".
 *
 * <p>{@link UpdateType#FULL} replaces the station's list atomically
 * (empty list clears it). {@link UpdateType#DIFFERENTIAL} uses per-entry
 * {@code idTokenInfo} presence to mean add/update vs remove.
 *
 * <p>The list parameter may be empty (meaningful only for Full updates);
 * it may not be {@code null}.
 */
public interface SendLocalListPort {

    CompletableFuture<SendLocalListResult> sendLocalList(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int versionNumber,
            UpdateType updateType,
            List<AuthorizationData> localAuthorizationList);
}
