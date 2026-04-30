package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppMessages;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.Direction;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.FrameType;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.LifecycleKind;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.OcppFrame;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.Lifecycle;
import com.evlibre.server.core.domain.shared.model.TenantId;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link OcppWebSocketVerticle} captures the full set of trace
 * entries — connect lifecycle, inbound CALL, outbound CALL_RESULT, disconnect
 * lifecycle — as a real station boots and disconnects.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class MessageTraceCaptureIT {

    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("TRACE-1");

    private OcppTestHarness harness;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void capturesConnectInboundOutboundDisconnect(Vertx vertx, VertxTestContext ctx) {
        harness.send16(vertx, STATION.value(), OcppMessages.bootNotification16("boot-1", "ABB", "Terra AC"))
                .whenComplete((response, err) -> {
                    if (err != null) {
                        ctx.failNow(err);
                        return;
                    }
                    // Give the close handler a tick to record the disconnect lifecycle entry
                    vertx.setTimer(50, id -> ctx.verify(() -> {
                        List<MessageTraceEntry> entries = harness.traceStore.recent(TENANT, STATION);

                        assertThat(entries).hasSizeGreaterThanOrEqualTo(4);

                        Lifecycle connected = (Lifecycle) entries.get(0);
                        assertThat(connected.kind()).isEqualTo(LifecycleKind.CONNECTED);
                        assertThat(connected.detail()).isEqualTo("ocpp1.6");

                        OcppFrame inbound = (OcppFrame) entries.get(1);
                        assertThat(inbound.direction()).isEqualTo(Direction.IN);
                        assertThat(inbound.type()).isEqualTo(FrameType.CALL);
                        assertThat(inbound.action()).isEqualTo("BootNotification");
                        assertThat(inbound.messageId()).isEqualTo("boot-1");
                        assertThat(inbound.rawPayload()).contains("BootNotification").contains("ABB");

                        OcppFrame outbound = (OcppFrame) entries.get(2);
                        assertThat(outbound.direction()).isEqualTo(Direction.OUT);
                        assertThat(outbound.type()).isEqualTo(FrameType.CALL_RESULT);
                        assertThat(outbound.messageId()).isEqualTo("boot-1");

                        Lifecycle disconnected = (Lifecycle) entries.get(entries.size() - 1);
                        assertThat(disconnected.kind()).isEqualTo(LifecycleKind.DISCONNECTED);

                        // Each store-record was paired with an event-publish
                        assertThat(harness.traceEvents.events).hasSameSizeAs(entries);
                        ctx.completeNow();
                    }));
                });
    }
}
