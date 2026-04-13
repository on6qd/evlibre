package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;
import com.evlibre.server.core.domain.dto.StatusNotificationData;
import com.evlibre.server.core.domain.model.*;
import com.evlibre.server.core.domain.ports.outbound.OcppEventLogPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HandleStatusNotificationUseCaseTest {

    private FakeEventLog eventLog;
    private HandleStatusNotificationUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity stationIdentity = new ChargePointIdentity("CHARGER-001");
    private final Instant fixedTime = Instant.parse("2025-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        eventLog = new FakeEventLog();
        useCase = new HandleStatusNotificationUseCase(eventLog);
    }

    @Test
    void status_notification_logs_event() {
        var data = new StatusNotificationData(
                tenantId, stationIdentity,
                new ConnectorId(1), ConnectorStatus.AVAILABLE, "NoError", fixedTime
        );

        useCase.statusNotification(data);

        assertThat(eventLog.events).hasSize(1);
        assertThat(eventLog.events.get(0)).contains("StatusNotification");
        assertThat(eventLog.events.get(0)).contains("CHARGER-001");
        assertThat(eventLog.events.get(0)).contains("IN");
    }

    @Test
    void status_notification_includes_connector_and_status_in_payload() {
        var data = new StatusNotificationData(
                tenantId, stationIdentity,
                new ConnectorId(2), ConnectorStatus.CHARGING, "NoError", fixedTime
        );

        useCase.statusNotification(data);

        assertThat(eventLog.events).hasSize(1);
        assertThat(eventLog.events.get(0)).contains("connector=2");
        assertThat(eventLog.events.get(0)).contains("status=CHARGING");
    }

    // --- Fakes ---

    static class FakeEventLog implements OcppEventLogPort {
        final List<String> events = new ArrayList<>();

        @Override
        public void logEvent(String stationIdentity, String messageId, String action,
                             String direction, String payload) {
            events.add(String.format("%s %s %s %s", stationIdentity, action, direction, payload));
        }
    }
}
