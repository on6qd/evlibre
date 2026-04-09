package com.evlibre.server.core.domain.ports.outbound;

public interface OcppEventLogPort {

    void logEvent(String stationIdentity, String messageId, String action,
                  String direction, String payload);
}
