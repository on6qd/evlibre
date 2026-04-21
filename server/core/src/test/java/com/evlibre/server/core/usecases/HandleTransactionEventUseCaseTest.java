package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;
import com.evlibre.common.model.EvseId;
import com.evlibre.server.core.domain.v201.dto.TransactionEventData;
import com.evlibre.server.core.domain.v201.dto.TransactionEventResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HandleTransactionEventUseCaseTest {

    private FakeEventLog eventLog;
    private HandleTransactionEventUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity stationIdentity = new ChargePointIdentity("CHARGER-001");
    private final Instant fixedTime = Instant.parse("2025-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        eventLog = new FakeEventLog();
        useCase = new HandleTransactionEventUseCase(eventLog);
    }

    @Test
    void transaction_event_started_logs_event() {
        var data = new TransactionEventData(
                tenantId, stationIdentity,
                "Started", "tx-abc-123", "TOKEN001", "CablePluggedIn",
                new EvseId(1), new ConnectorId(1), fixedTime, List.of()
        );

        useCase.transactionEvent(data);

        assertThat(eventLog.events).hasSize(1);
        assertThat(eventLog.events.get(0)).contains("TransactionEvent");
        assertThat(eventLog.events.get(0)).contains("CHARGER-001");
        assertThat(eventLog.events.get(0)).contains("eventType=Started");
        assertThat(eventLog.events.get(0)).contains("tx=tx-abc-123");
    }

    @Test
    void transaction_event_returns_zero_total_cost() {
        var data = new TransactionEventData(
                tenantId, stationIdentity,
                "Updated", "tx-abc-123", "TOKEN001", "MeterValuePeriodic",
                new EvseId(1), new ConnectorId(1), fixedTime, List.of()
        );

        TransactionEventResult result = useCase.transactionEvent(data);

        assertThat(result.totalCost()).isZero();
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
