package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GetTransactionStatusResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetTransactionStatusUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private GetTransactionStatusUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new GetTransactionStatusUseCaseV201(commandSender);
    }

    @Test
    void payload_includes_transaction_id_when_supplied() {
        commandSender.setNextResponse(Map.of("ongoingIndicator", true, "messagesInQueue", false));

        useCase.getTransactionStatus(tenantId, station, "tx-abc").join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("GetTransactionStatus");
        assertThat(cmd.payload()).containsEntry("transactionId", "tx-abc");
    }

    @Test
    void payload_omits_transaction_id_when_null() {
        commandSender.setNextResponse(Map.of("messagesInQueue", true));

        useCase.getTransactionStatus(tenantId, station, null).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload()).doesNotContainKey("transactionId");
    }

    @Test
    void ongoing_true_with_messages_false() {
        commandSender.setNextResponse(Map.of("ongoingIndicator", true, "messagesInQueue", false));

        GetTransactionStatusResult r = useCase.getTransactionStatus(tenantId, station, "tx-1").join();

        assertThat(r.isOngoing()).isTrue();
        assertThat(r.hasOngoingIndicator()).isTrue();
        assertThat(r.messagesInQueue()).isFalse();
    }

    @Test
    void ongoing_false_with_messages_true() {
        commandSender.setNextResponse(Map.of("ongoingIndicator", false, "messagesInQueue", true));

        GetTransactionStatusResult r = useCase.getTransactionStatus(tenantId, station, "tx-2").join();

        assertThat(r.isOngoing()).isFalse();
        assertThat(r.hasOngoingIndicator()).isTrue();
        assertThat(r.messagesInQueue()).isTrue();
    }

    @Test
    void unknown_transaction_reports_both_false() {
        commandSender.setNextResponse(Map.of("ongoingIndicator", false, "messagesInQueue", false));

        GetTransactionStatusResult r = useCase.getTransactionStatus(tenantId, station, "unknown-tx").join();

        assertThat(r.isOngoing()).isFalse();
        assertThat(r.messagesInQueue()).isFalse();
    }

    @Test
    void absent_ongoing_indicator_preserves_null_when_request_had_no_transaction_id() {
        commandSender.setNextResponse(Map.of("messagesInQueue", true));

        GetTransactionStatusResult r = useCase.getTransactionStatus(tenantId, station, null).join();

        assertThat(r.hasOngoingIndicator()).isFalse();
        assertThat(r.ongoingIndicator()).isNull();
        assertThat(r.isOngoing()).isFalse();
        assertThat(r.messagesInQueue()).isTrue();
    }

    @Test
    void missing_messages_in_queue_rejected() {
        Map<String, Object> response = new HashMap<>();
        response.put("ongoingIndicator", true);
        commandSender.setNextResponse(response);

        assertThatThrownBy(() -> useCase.getTransactionStatus(tenantId, station, "tx-x").join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("messagesInQueue");
    }
}
