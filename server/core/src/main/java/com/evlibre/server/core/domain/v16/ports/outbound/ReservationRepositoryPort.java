package com.evlibre.server.core.domain.v16.ports.outbound;

import com.evlibre.server.core.domain.v16.model.Reservation;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.util.Optional;

public interface ReservationRepositoryPort {

    void save(Reservation reservation);

    Optional<Reservation> findByReservationId(TenantId tenantId, int reservationId);

    int nextReservationId();
}
