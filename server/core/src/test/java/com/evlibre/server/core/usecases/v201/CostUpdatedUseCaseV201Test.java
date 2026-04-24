package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CostUpdatedUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private CostUpdatedUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new CostUpdatedUseCaseV201(commandSender);
    }

    @Test
    void null_transactionId_rejected() {
        assertThatThrownBy(() -> useCase.costUpdated(tenantId, station, 1.00, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("transactionId");
    }

    @Test
    void transactionId_over_36_chars_rejected() {
        String tooLong = "x".repeat(37);

        assertThatThrownBy(() -> useCase.costUpdated(tenantId, station, 1.00, tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("36");
    }

    @Test
    void payload_contains_totalCost_and_transactionId() {
        commandSender.setNextResponse(Map.of());

        useCase.costUpdated(tenantId, station, 12.50, "tx-001").join();

        Map<String, Object> payload = commandSender.commands().get(0).payload();
        assertThat(payload)
                .containsEntry("totalCost", 12.50)
                .containsEntry("transactionId", "tx-001");
    }

    @Test
    void negative_total_cost_is_allowed_for_refund_scenarios() {
        commandSender.setNextResponse(Map.of());

        useCase.costUpdated(tenantId, station, -5.25, "tx-refund").join();

        assertThat(commandSender.commands().get(0).payload())
                .containsEntry("totalCost", -5.25);
    }

    @Test
    void empty_response_ack_completes_future_normally() {
        commandSender.setNextResponse(Map.of());

        Void result = useCase.costUpdated(tenantId, station, 0.0, "tx-free").join();

        assertThat(result).isNull();
    }

    @Test
    void boundary_transactionId_length_accepted() {
        String boundary = "x".repeat(36);
        commandSender.setNextResponse(Map.of());

        useCase.costUpdated(tenantId, station, 1.0, boundary).join();

        assertThat(commandSender.commands().get(0).payload())
                .containsEntry("transactionId", boundary);
    }
}
