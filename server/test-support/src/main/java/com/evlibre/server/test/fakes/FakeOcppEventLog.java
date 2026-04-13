package com.evlibre.server.test.fakes;

import com.evlibre.server.core.domain.ports.outbound.OcppEventLogPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakeOcppEventLog implements OcppEventLogPort {

    public record LogEntry(String stationIdentity, String messageId, String action,
                           String direction, String payload) {}

    private final List<LogEntry> entries = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void logEvent(String stationIdentity, String messageId, String action,
                         String direction, String payload) {
        entries.add(new LogEntry(stationIdentity, messageId, action, direction, payload));
    }

    public List<LogEntry> entries() {
        return List.copyOf(entries);
    }

    public boolean hasEventForAction(String action) {
        return entries.stream().anyMatch(e -> action.equals(e.action()));
    }

    public void clear() {
        entries.clear();
    }
}
