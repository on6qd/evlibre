package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;
import com.evlibre.common.model.MeterValue;
import com.evlibre.common.model.SampledValue;
import com.evlibre.server.core.domain.v16.dto.MeterValuesData;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HandleMeterValuesUseCaseTest {

    private FakeEventLog eventLog;
    private HandleMeterValuesUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity stationIdentity = new ChargePointIdentity("CHARGER-001");
    private final Instant fixedTime = Instant.parse("2025-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        eventLog = new FakeEventLog();
        useCase = new HandleMeterValuesUseCase(eventLog);
    }

    @Test
    void meter_values_logs_event() {
        var meterValues = List.of(
                new MeterValue(fixedTime, List.of(
                        new SampledValue("1500", "Sample.Periodic", "Raw", "Energy.Active.Import.Register", null, "Outlet", "Wh")
                ))
        );
        var data = new MeterValuesData(tenantId, stationIdentity, new ConnectorId(1), null, meterValues);

        useCase.meterValues(data);

        assertThat(eventLog.events).hasSize(1);
        assertThat(eventLog.events.get(0)).contains("MeterValues");
        assertThat(eventLog.events.get(0)).contains("CHARGER-001");
        assertThat(eventLog.events.get(0)).contains("readings=1");
    }

    @Test
    void meter_values_with_transaction_id_includes_it_in_log() {
        var meterValues = List.of(
                new MeterValue(fixedTime, List.of(
                        new SampledValue("2000", "Sample.Periodic", "Raw", "Energy.Active.Import.Register", null, "Outlet", "Wh")
                )),
                new MeterValue(fixedTime.plusSeconds(60), List.of(
                        new SampledValue("2100", "Sample.Periodic", "Raw", "Energy.Active.Import.Register", null, "Outlet", "Wh")
                ))
        );
        var data = new MeterValuesData(tenantId, stationIdentity, new ConnectorId(1), 42, meterValues);

        useCase.meterValues(data);

        assertThat(eventLog.events).hasSize(1);
        assertThat(eventLog.events.get(0)).contains("tx=42");
        assertThat(eventLog.events.get(0)).contains("readings=2");
    }

    // OCPP 1.6 §5.27 / errata: the CP uses transactionId=-1 when StartTransaction.conf was lost.
    // The CSMS SHALL accept MeterValues with tx=-1 as if valid — no error, no rejection.
    @Test
    void meter_values_with_minus_one_transaction_id_is_accepted() {
        var meterValues = List.of(
                new MeterValue(fixedTime, List.of(
                        new SampledValue("1500", null, null, null, null, null, "Wh")))
        );
        var data = new MeterValuesData(tenantId, stationIdentity, new ConnectorId(1), -1, meterValues);

        useCase.meterValues(data);

        assertThat(eventLog.events).hasSize(1);
        assertThat(eventLog.events.get(0)).contains("tx=-1");
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
