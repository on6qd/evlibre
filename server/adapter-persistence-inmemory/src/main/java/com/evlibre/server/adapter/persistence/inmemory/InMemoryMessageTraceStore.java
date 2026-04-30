package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.MessageTraceStorePort;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMessageTraceStore implements MessageTraceStorePort {

    public static final int DEFAULT_CAPACITY = 500;

    private final int capacity;
    private final Map<Key, Deque<MessageTraceEntry>> byStation = new ConcurrentHashMap<>();

    public InMemoryMessageTraceStore() {
        this(DEFAULT_CAPACITY);
    }

    public InMemoryMessageTraceStore(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    @Override
    public void record(TenantId tenant, ChargePointIdentity station, MessageTraceEntry entry) {
        Deque<MessageTraceEntry> ring = byStation.computeIfAbsent(new Key(tenant, station),
                k -> new ArrayDeque<>(capacity));
        synchronized (ring) {
            if (ring.size() == capacity) {
                ring.removeFirst();
            }
            ring.addLast(entry);
        }
    }

    @Override
    public List<MessageTraceEntry> recent(TenantId tenant, ChargePointIdentity station) {
        Deque<MessageTraceEntry> ring = byStation.get(new Key(tenant, station));
        if (ring == null) {
            return List.of();
        }
        synchronized (ring) {
            return List.copyOf(ring);
        }
    }

    private record Key(TenantId tenant, ChargePointIdentity station) {}
}
