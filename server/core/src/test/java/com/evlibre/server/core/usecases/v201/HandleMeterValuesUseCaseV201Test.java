package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.EvseId;
import com.evlibre.common.model.MeterValue;
import com.evlibre.common.model.SampledValue;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import com.evlibre.server.core.domain.v201.dto.MeterValuesData201;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HandleMeterValuesUseCaseV201Test {

    private FakeEventLog eventLog;
    private HandleMeterValuesUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity stationIdentity = new ChargePointIdentity("CHARGER-001");
    private final Instant fixedTime = Instant.parse("2026-04-24T10:00:00Z");

    @BeforeEach
    void setUp() {
        eventLog = new FakeEventLog();
        useCase = new HandleMeterValuesUseCaseV201(eventLog);
    }

    @Test
    void meter_values_event_carries_evse_and_reading_count() {
        var data = new MeterValuesData201(tenantId, stationIdentity, new EvseId(2),
                List.of(sample("1200"), sample("1234")));

        useCase.meterValues(data);

        assertThat(eventLog.events).hasSize(1);
        var event = eventLog.events.get(0);
        assertThat(event).contains("CHARGER-001");
        assertThat(event).contains("MeterValues");
        assertThat(event).contains("IN");
        assertThat(event).contains("evse=2");
        assertThat(event).contains("readings=2");
    }

    private static MeterValue sample(String value) {
        return new MeterValue(Instant.parse("2026-04-24T10:00:00Z"),
                List.of(new SampledValue(value, null, null, null, null, null, "Wh")));
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
