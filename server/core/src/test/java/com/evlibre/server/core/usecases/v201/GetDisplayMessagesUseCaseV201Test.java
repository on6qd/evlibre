package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.displaymessage.GetDisplayMessagesResult;
import com.evlibre.server.core.domain.v201.displaymessage.GetDisplayMessagesStatus;
import com.evlibre.server.core.domain.v201.displaymessage.MessagePriority;
import com.evlibre.server.core.domain.v201.displaymessage.MessageState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetDisplayMessagesUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private GetDisplayMessagesUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new GetDisplayMessagesUseCaseV201(commandSender);
    }

    @Test
    void null_ids_rejected() {
        assertThatThrownBy(() -> useCase.getDisplayMessages(tenantId, station, 1, null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ids");
    }

    @Test
    void null_id_in_list_rejected() {
        assertThatThrownBy(() -> useCase.getDisplayMessages(tenantId, station, 1,
                Arrays.asList(1, null, 3), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    void empty_filters_only_requestId_on_wire() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.getDisplayMessages(tenantId, station, 42, List.of(), null, null).join();

        Map<String, Object> payload = commandSender.commands().get(0).payload();
        assertThat(payload)
                .containsEntry("requestId", 42)
                .doesNotContainKey("id")
                .doesNotContainKey("priority")
                .doesNotContainKey("state");
    }

    @Test
    void ids_and_enums_serialised_as_arrays_and_pascal_case() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.getDisplayMessages(tenantId, station, 42,
                List.of(1, 2, 3), MessagePriority.ALWAYS_FRONT, MessageState.CHARGING).join();

        Map<String, Object> payload = commandSender.commands().get(0).payload();
        assertThat(payload)
                .containsEntry("id", List.of(1, 2, 3))
                .containsEntry("priority", "AlwaysFront")
                .containsEntry("state", "Charging");
    }

    @Test
    void both_response_statuses_decoded() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));
        assertThat(useCase.getDisplayMessages(tenantId, station, 1, List.of(), null, null).join().status())
                .isEqualTo(GetDisplayMessagesStatus.ACCEPTED);

        commandSender.setNextResponse(Map.of("status", "Unknown"));
        GetDisplayMessagesResult r = useCase.getDisplayMessages(tenantId, station, 1,
                List.of(), null, null).join();
        assertThat(r.status()).isEqualTo(GetDisplayMessagesStatus.UNKNOWN);
        assertThat(r.isAccepted()).isFalse();
    }

    @Test
    void statusInfo_reasonCode_surfaced() {
        commandSender.setNextResponse(Map.of(
                "status", "Unknown",
                "statusInfo", Map.of("reasonCode", "NoMatchingMessages")));

        GetDisplayMessagesResult r = useCase.getDisplayMessages(tenantId, station, 1,
                List.of(), null, null).join();

        assertThat(r.statusInfoReason()).isEqualTo("NoMatchingMessages");
    }

    @Test
    void unknown_wire_status_raises() {
        commandSender.setNextResponse(Map.of("status", "FutureStatus"));

        assertThatThrownBy(() -> useCase.getDisplayMessages(tenantId, station, 1,
                List.of(), null, null).join())
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
