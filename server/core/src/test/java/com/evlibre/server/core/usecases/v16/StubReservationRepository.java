package com.evlibre.server.core.usecases.v16;

import com.evlibre.server.core.domain.v16.model.Reservation;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.outbound.ReservationRepositoryPort;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class StubReservationRepository implements ReservationRepositoryPort {

    private final Map<String, Reservation> store = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(1);

    @Override
    public void save(Reservation reservation) {
        store.put(key(reservation.tenantId(), reservation.reservationId()), reservation);
    }

    @Override
    public Optional<Reservation> findByReservationId(TenantId tenantId, int reservationId) {
        return Optional.ofNullable(store.get(key(tenantId, reservationId)));
    }

    @Override
    public int nextReservationId() {
        return idSequence.getAndIncrement();
    }

    private String key(TenantId tenantId, int reservationId) {
        return tenantId.value() + ":" + reservationId;
    }
}
