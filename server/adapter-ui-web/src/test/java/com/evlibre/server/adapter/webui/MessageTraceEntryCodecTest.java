package com.evlibre.server.adapter.webui;

import com.evlibre.server.core.domain.shared.model.MessageTraceEntry;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.Direction;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.FrameType;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.Lifecycle;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.LifecycleKind;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.OcppFrame;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTraceEntryCodecTest {

    @Test
    void roundtrip_ocppFrame() {
        OcppFrame original = new OcppFrame(
                Instant.parse("2026-04-30T10:42:33.421Z"),
                Direction.IN,
                FrameType.CALL,
                "BootNotification",
                "msg-1",
                "{\"chargingStation\":{\"vendorName\":\"eNovates\"}}");

        JsonObject json = MessageTraceEntryCodec.encode(original);
        MessageTraceEntry decoded = MessageTraceEntryCodec.decode(json);

        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void roundtrip_lifecycle() {
        Lifecycle original = new Lifecycle(
                Instant.parse("2026-04-30T10:42:35.012Z"),
                LifecycleKind.SUBPROTOCOL_REJECTED,
                "ocpp2.01");

        JsonObject json = MessageTraceEntryCodec.encode(original);
        MessageTraceEntry decoded = MessageTraceEntryCodec.decode(json);

        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void encode_marksKind() {
        assertThat(MessageTraceEntryCodec.encode(
                new OcppFrame(Instant.now(), Direction.OUT, FrameType.CALL_RESULT, null, "x", "{}"))
                .getString("kind")).isEqualTo("frame");
        assertThat(MessageTraceEntryCodec.encode(
                new Lifecycle(Instant.now(), LifecycleKind.CONNECTED, "ocpp2.0.1"))
                .getString("kind")).isEqualTo("lifecycle");
    }
}
