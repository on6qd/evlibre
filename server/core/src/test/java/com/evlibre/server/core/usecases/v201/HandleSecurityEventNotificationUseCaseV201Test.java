package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.security.SecurityEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HandleSecurityEventNotificationUseCaseV201Test {

    private static final TenantId TENANT = new TenantId("demo");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("SEC-1");

    @Test
    void forwards_event_to_sink() {
        List<SecurityEvent> captured = new ArrayList<>();
        HandleSecurityEventNotificationUseCaseV201 useCase =
                new HandleSecurityEventNotificationUseCaseV201((t, s, e) -> captured.add(e));

        SecurityEvent event = new SecurityEvent("FirmwareUpdated",
                Instant.parse("2027-05-01T10:00:00Z"),
                "fw=2.4.1");

        useCase.handleSecurityEventNotification(TENANT, STATION, event);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0)).isEqualTo(event);
    }

    @Test
    void rejects_null_tenant() {
        HandleSecurityEventNotificationUseCaseV201 useCase =
                new HandleSecurityEventNotificationUseCaseV201((t, s, e) -> {});
        SecurityEvent event = SecurityEvent.of("Reset", Instant.now());
        assertThatThrownBy(() ->
                useCase.handleSecurityEventNotification(null, STATION, event))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_null_sink_at_construction() {
        assertThatThrownBy(() -> new HandleSecurityEventNotificationUseCaseV201(null))
                .isInstanceOf(NullPointerException.class);
    }
}
