package com.evlibre.server.core.domain.ports.outbound;

import com.evlibre.server.core.domain.model.Reservation;
import com.evlibre.server.core.domain.model.TenantId;

import java.util.Optional;

public interface ReservationRepositoryPort {

    void save(Reservation reservation);

    Optional<Reservation> findByReservationId(TenantId tenantId, int reservationId);

    int nextReservationId();
}
