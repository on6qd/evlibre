package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.RequestStartStopStatus;
import com.evlibre.server.core.domain.v201.dto.RequestStopTransactionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestStopTransactionUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private RequestStopTransactionUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new RequestStopTransactionUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    @Test
    void payload_carries_only_transaction_id() {
        useCase.requestStopTransaction(tenantId, station, "tx-uuid-42").join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("RequestStopTransaction");
        assertThat(cmd.payload()).containsOnly(Map.entry("transactionId", "tx-uuid-42"));
    }

    @Test
    void accepted_status_parsed() {
        RequestStopTransactionResult result = useCase.requestStopTransaction(tenantId, station, "tx").join();

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.status()).isEqualTo(RequestStartStopStatus.ACCEPTED);
        assertThat(result.statusInfoReason()).isNull();
    }

    @Test
    void rejected_surfaces_status_info_reason() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "NoTransaction")));

        RequestStopTransactionResult result = useCase.requestStopTransaction(tenantId, station, "nope").join();

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.status()).isEqualTo(RequestStartStopStatus.REJECTED);
        assertThat(result.statusInfoReason()).isEqualTo("NoTransaction");
    }

    @Test
    void unknown_status_rejected() {
        commandSender.setNextResponse(Map.of("status", "Pending"));

        assertThatThrownBy(() -> useCase.requestStopTransaction(tenantId, station, "tx").join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Pending");
    }

    @Test
    void null_transaction_id_rejected() {
        assertThatThrownBy(() -> useCase.requestStopTransaction(tenantId, station, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("transactionId");
    }

    @Test
    void blank_transaction_id_rejected() {
        assertThatThrownBy(() -> useCase.requestStopTransaction(tenantId, station, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transactionId");
    }
}
