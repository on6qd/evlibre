package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryOcppEventLog implements OcppEventLogPort {

    private final List<OcppEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void logEvent(String stationIdentity, String messageId, String action,
                         String direction, String payload) {
        events.add(new OcppEvent(stationIdentity, messageId, action, direction, payload));
    }

    public List<OcppEvent> events() {
        return List.copyOf(events);
    }

    public record OcppEvent(String stationIdentity, String messageId, String action,
                             String direction, String payload) {}
}
