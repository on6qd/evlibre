package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleSecurityEventNotificationPort;
import com.evlibre.server.core.domain.v201.security.SecurityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class HandleSecurityEventNotificationUseCaseV201 implements HandleSecurityEventNotificationPort {

    @FunctionalInterface
    public interface Sink {
        void onSecurityEvent(TenantId tenantId,
                             ChargePointIdentity stationIdentity,
                             SecurityEvent event);
    }

    private static final Logger log = LoggerFactory.getLogger(HandleSecurityEventNotificationUseCaseV201.class);

    private final Sink sink;

    public HandleSecurityEventNotificationUseCaseV201(Sink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    @Override
    public void handleSecurityEventNotification(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            SecurityEvent event) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(event, "event");
        log.info("SecurityEventNotification from {} (type={}, at={})",
                stationIdentity.value(), event.type(), event.timestamp());
        sink.onSecurityEvent(tenantId, stationIdentity, event);
    }
}
