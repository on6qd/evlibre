package com.evlibre.server.core.domain.v201.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestStartTransactionResultTest {

    @Test
    void accepted_with_transaction_id_echo() {
        var r = new RequestStartTransactionResult(
                RequestStartStopStatus.ACCEPTED, "tx-uuid", null,
                Map.of("status", "Accepted", "transactionId", "tx-uuid"));

        assertThat(r.isAccepted()).isTrue();
        assertThat(r.transactionId()).isEqualTo("tx-uuid");
        assertThat(r.statusInfoReason()).isNull();
    }

    @Test
    void rejected_carries_status_info_reason() {
        var r = new RequestStartTransactionResult(
                RequestStartStopStatus.REJECTED, null, "UnknownEvse", Map.of());

        assertThat(r.isAccepted()).isFalse();
        assertThat(r.statusInfoReason()).isEqualTo("UnknownEvse");
    }

    @Test
    void null_raw_response_normalised_to_empty_map() {
        var r = new RequestStartTransactionResult(
                RequestStartStopStatus.ACCEPTED, null, null, null);

        assertThat(r.rawResponse()).isEmpty();
    }

    @Test
    void null_status_rejected() {
        assertThatThrownBy(() -> new RequestStartTransactionResult(null, null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("status");
    }
}
