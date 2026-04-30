package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.MessageTraceStorePort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakeMessageTraceStore implements MessageTraceStorePort {

    private final Map<Key, List<MessageTraceEntry>> byStation = new HashMap<>();

    @Override
    public synchronized void record(TenantId tenant, ChargePointIdentity station, MessageTraceEntry entry) {
        byStation.computeIfAbsent(new Key(tenant, station), k -> new ArrayList<>()).add(entry);
    }

    @Override
    public synchronized List<MessageTraceEntry> recent(TenantId tenant, ChargePointIdentity station) {
        List<MessageTraceEntry> entries = byStation.get(new Key(tenant, station));
        return entries == null ? List.of() : List.copyOf(entries);
    }

    private record Key(TenantId tenant, ChargePointIdentity station) {}
}
