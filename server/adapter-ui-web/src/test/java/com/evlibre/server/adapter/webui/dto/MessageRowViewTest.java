package com.evlibre.server.adapter.webui.dto;

import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.Direction;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.FrameType;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.Lifecycle;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.LifecycleKind;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.OcppFrame;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MessageRowViewTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void formatsTimeInUtc() {
        OcppFrame f = new OcppFrame(
                Instant.parse("2026-04-30T10:42:33.421Z"),
                Direction.IN, FrameType.CALL, "BootNotification", "msg-1", "{}");

        MessageRowView v = MessageRowView.from(f, mapper);

        assertThat(v.formattedTime()).isEqualTo("10:42:33.421");
    }

    @Test
    void inboundFrameUsesArrowIn() {
        OcppFrame f = new OcppFrame(Instant.now(), Direction.IN, FrameType.CALL,
                "Heartbeat", "msg-2", "{}");

        MessageRowView v = MessageRowView.from(f, mapper);

        assertThat(v.lifecycle()).isFalse();
        assertThat(v.direction()).isEqualTo("→");
        assertThat(v.typeLabel()).isEqualTo("CALL");
        assertThat(v.typeBadgeClass()).contains("border-info").contains("text-info");
        assertThat(v.action()).isEqualTo("Heartbeat");
        assertThat(v.messageId()).isEqualTo("msg-2");
    }

    @Test
    void outboundFrameUsesArrowOut() {
        OcppFrame f = new OcppFrame(Instant.now(), Direction.OUT, FrameType.CALL_RESULT,
                null, "msg-3", "{}");

        MessageRowView v = MessageRowView.from(f, mapper);

        assertThat(v.direction()).isEqualTo("←");
        assertThat(v.typeLabel()).isEqualTo("RESULT");
        assertThat(v.typeBadgeClass()).contains("border-base-content").contains("text-base-content");
        assertThat(v.action()).isEmpty();
    }

    @Test
    void callErrorBadgeIsErrorClass() {
        OcppFrame f = new OcppFrame(Instant.now(), Direction.OUT, FrameType.CALL_ERROR,
                null, "msg-4", "[\"NotImplemented\",\"x\",{}]");

        MessageRowView v = MessageRowView.from(f, mapper);

        assertThat(v.typeLabel()).isEqualTo("ERROR");
        assertThat(v.typeBadgeClass()).contains("border-error").contains("text-error");
    }

    @Test
    void prettyPrintsJsonPayload() {
        OcppFrame f = new OcppFrame(Instant.now(), Direction.IN, FrameType.CALL,
                "BootNotification", "msg-5",
                "[2,\"msg-5\",\"BootNotification\",{\"chargingStation\":{\"vendorName\":\"eNovates\"}}]");

        MessageRowView v = MessageRowView.from(f, mapper);

        assertThat(v.prettyPayload()).contains("\n").contains("\"vendorName\" : \"eNovates\"");
    }

    @Test
    void malformedPayloadIsReturnedAsIs() {
        OcppFrame f = new OcppFrame(Instant.now(), Direction.IN, FrameType.CALL,
                "x", "y", "not-json");

        MessageRowView v = MessageRowView.from(f, mapper);

        assertThat(v.prettyPayload()).isEqualTo("not-json");
    }

    @Test
    void lifecycleConnected() {
        Lifecycle l = new Lifecycle(Instant.now(), LifecycleKind.CONNECTED, "ocpp2.0.1");

        MessageRowView v = MessageRowView.from(l, mapper);

        assertThat(v.lifecycle()).isTrue();
        assertThat(v.lifecycleLabel()).isEqualTo("connected (ocpp2.0.1)");
    }

    @Test
    void lifecycleSubprotocolRejected() {
        Lifecycle l = new Lifecycle(Instant.now(), LifecycleKind.SUBPROTOCOL_REJECTED,
                "offered=[ocpp2.01]");

        MessageRowView v = MessageRowView.from(l, mapper);

        assertThat(v.lifecycleLabel()).isEqualTo("sub-protocol rejected (offered=[ocpp2.01])");
    }

    @Test
    void lifecycleWithBlankDetail() {
        Lifecycle l = new Lifecycle(Instant.now(), LifecycleKind.DISCONNECTED, "");

        MessageRowView v = MessageRowView.from(l, mapper);

        assertThat(v.lifecycleLabel()).isEqualTo("disconnected");
    }
}
