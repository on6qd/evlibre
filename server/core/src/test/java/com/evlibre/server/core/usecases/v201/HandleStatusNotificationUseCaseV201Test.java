package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;
import com.evlibre.common.model.EvseId;
import com.evlibre.server.core.domain.shared.model.ConnectorStatus;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import com.evlibre.server.core.domain.v201.dto.StatusNotificationData201;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HandleStatusNotificationUseCaseV201Test {

    private FakeEventLog eventLog;
    private HandleStatusNotificationUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity stationIdentity = new ChargePointIdentity("CHARGER-001");
    private final Instant fixedTime = Instant.parse("2026-04-24T10:00:00Z");

    @BeforeEach
    void setUp() {
        eventLog = new FakeEventLog();
        useCase = new HandleStatusNotificationUseCaseV201(eventLog);
    }

    @Test
    void status_notification_event_carries_evse_and_connector() {
        var data = new StatusNotificationData201(
                tenantId, stationIdentity,
                new EvseId(2), new ConnectorId(1),
                ConnectorStatus.AVAILABLE, fixedTime);

        useCase.statusNotification(data);

        assertThat(eventLog.events).hasSize(1);
        var event = eventLog.events.get(0);
        assertThat(event).contains("CHARGER-001");
        assertThat(event).contains("StatusNotification");
        assertThat(event).contains("IN");
        assertThat(event).contains("evse=2");
        assertThat(event).contains("connector=1");
        assertThat(event).contains("status=AVAILABLE");
    }

    @Test
    void station_wide_evse_zero_logged_verbatim() {
        var data = new StatusNotificationData201(
                tenantId, stationIdentity,
                new EvseId(0), new ConnectorId(0),
                ConnectorStatus.UNAVAILABLE, fixedTime);

        useCase.statusNotification(data);

        assertThat(eventLog.events.get(0)).contains("evse=0 connector=0 status=UNAVAILABLE");
    }

    static class FakeEventLog implements OcppEventLogPort {
        final List<String> events = new ArrayList<>();

        @Override
        public void logEvent(String stationIdentity, String messageId, String action,
                             String direction, String payload) {
            events.add(String.format("%s %s %s %s", stationIdentity, action, direction, payload));
        }
    }
}
