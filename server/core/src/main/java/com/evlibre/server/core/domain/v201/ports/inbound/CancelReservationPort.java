package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.CancelReservationResult;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code CancelReservation} for OCPP 2.0.1 (use case H02).
 * Removes a reservation previously established via {@code ReserveNow}. The
 * {@code reservationId} matches the {@code id} supplied in that earlier
 * request; it is scoped station-wide.
 */
public interface CancelReservationPort {

    CompletableFuture<CancelReservationResult> cancelReservation(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int reservationId);
}
