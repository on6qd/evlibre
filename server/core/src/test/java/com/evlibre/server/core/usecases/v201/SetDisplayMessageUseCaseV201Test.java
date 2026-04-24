package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.displaymessage.MessageInfo;
import com.evlibre.server.core.domain.v201.displaymessage.MessagePriority;
import com.evlibre.server.core.domain.v201.displaymessage.MessageState;
import com.evlibre.server.core.domain.v201.displaymessage.SetDisplayMessageResult;
import com.evlibre.server.core.domain.v201.displaymessage.SetDisplayMessageStatus;
import com.evlibre.server.core.domain.v201.model.MessageContent;
import com.evlibre.server.core.domain.v201.model.MessageFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SetDisplayMessageUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private SetDisplayMessageUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new SetDisplayMessageUseCaseV201(commandSender);
    }

    @Test
    void negative_id_rejected_at_construction() {
        assertThatThrownBy(() -> new MessageInfo(-1, MessagePriority.IN_FRONT,
                MessageContent.of(MessageFormat.UTF8, "hi"), null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }

    @Test
    void transactionId_over_36_chars_rejected_at_construction() {
        String tooLong = "x".repeat(37);
        assertThatThrownBy(() -> new MessageInfo(1, MessagePriority.IN_FRONT,
                MessageContent.of(MessageFormat.UTF8, "hi"), null, null, null, null, tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("36");
    }

    @Test
    void null_message_rejected() {
        assertThatThrownBy(() -> useCase.setDisplayMessage(tenantId, station, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("message");
    }

    @Test
    void minimal_message_required_fields_only_on_wire() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.setDisplayMessage(tenantId, station, MessageInfo.of(
                5, MessagePriority.NORMAL_CYCLE,
                MessageContent.of(MessageFormat.UTF8, "Plug in for a free charge"))).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> wire = (Map<String, Object>) commandSender.commands().get(0).payload().get("message");
        assertThat(wire)
                .containsEntry("id", 5)
                .containsEntry("priority", "NormalCycle")
                .doesNotContainKey("state")
                .doesNotContainKey("display")
                .doesNotContainKey("startDateTime")
                .doesNotContainKey("endDateTime")
                .doesNotContainKey("transactionId");
    }

    @Test
    void full_message_with_all_optional_fields_serialised() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.setDisplayMessage(tenantId, station, new MessageInfo(
                7, MessagePriority.ALWAYS_FRONT,
                new MessageContent(MessageFormat.HTML, "en", "<b>Charging</b>"),
                MessageState.CHARGING,
                new Component("Display", "primary", Evse.of(1)),
                Instant.parse("2026-04-24T10:00:00Z"),
                Instant.parse("2026-04-24T12:00:00Z"),
                "tx-abc-123")).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> wire = (Map<String, Object>) commandSender.commands().get(0).payload().get("message");
        assertThat(wire)
                .containsEntry("id", 7)
                .containsEntry("priority", "AlwaysFront")
                .containsEntry("state", "Charging")
                .containsEntry("startDateTime", "2026-04-24T10:00:00Z")
                .containsEntry("endDateTime", "2026-04-24T12:00:00Z")
                .containsEntry("transactionId", "tx-abc-123");
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) wire.get("message");
        assertThat(content)
                .containsEntry("format", "HTML")
                .containsEntry("language", "en")
                .containsEntry("content", "<b>Charging</b>");
        assertThat(wire).containsKey("display");
    }

    @Test
    void all_six_response_statuses_decoded() {
        for (String wire : new String[] {
                "Accepted", "Rejected",
                "NotSupportedMessageFormat", "NotSupportedPriority",
                "NotSupportedState", "UnknownTransaction"}) {
            commandSender.setNextResponse(Map.of("status", wire));
            SetDisplayMessageResult r = useCase.setDisplayMessage(tenantId, station,
                    MessageInfo.of(1, MessagePriority.IN_FRONT,
                            MessageContent.of(MessageFormat.UTF8, "x"))).join();
            assertThat(r.status()).isNotNull();
        }
    }

    @Test
    void accepted_marks_isAccepted_true() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        SetDisplayMessageResult r = useCase.setDisplayMessage(tenantId, station,
                MessageInfo.of(1, MessagePriority.IN_FRONT,
                        MessageContent.of(MessageFormat.UTF8, "hi"))).join();

        assertThat(r.isAccepted()).isTrue();
        assertThat(r.status()).isEqualTo(SetDisplayMessageStatus.ACCEPTED);
    }

    @Test
    void statusInfo_reasonCode_surfaced() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "NoFreeSlot")));

        SetDisplayMessageResult r = useCase.setDisplayMessage(tenantId, station,
                MessageInfo.of(1, MessagePriority.IN_FRONT,
                        MessageContent.of(MessageFormat.UTF8, "hi"))).join();

        assertThat(r.statusInfoReason()).isEqualTo("NoFreeSlot");
    }

    @Test
    void unknown_wire_status_raises() {
        commandSender.setNextResponse(Map.of("status", "FutureStatus"));

        assertThatThrownBy(() -> useCase.setDisplayMessage(tenantId, station,
                MessageInfo.of(1, MessagePriority.IN_FRONT,
                        MessageContent.of(MessageFormat.UTF8, "hi"))).join())
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
