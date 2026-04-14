package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.server.core.domain.model.Reservation;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.ReservationRepositoryPort;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryReservationRepository implements ReservationRepositoryPort {

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
