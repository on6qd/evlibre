package com.evlibre.server.adapter.ocpp;

import com.evlibre.server.adapter.ocpp.testutil.OcppMessages;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.v201.diagnostics.EventNotificationType;
import com.evlibre.server.core.domain.v201.diagnostics.EventTrigger;
import com.evlibre.server.core.domain.v201.diagnostics.UploadLogStatus;
import com.evlibre.server.test.fakes.FakeLogStatusSink;
import com.evlibre.server.test.fakes.FakeNotifyEventSink;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the Phase 6 diagnostics inbound flows (block N01).
 * Exercises wire→domain decoding plus dispatcher registration through
 * {@link OcppTestHarness}; payloads are schema-validated by
 * {@code OcppSchemaValidator} in hard-reject mode.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class Diagnostics201IT {

    private OcppTestHarness harness;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void log_status_uploading_with_request_id(Vertx vertx, VertxTestContext ctx) {
        String msg = """
                [2,"lsn-1","LogStatusNotification",{
                  "status": "Uploading",
                  "requestId": 11
                }]""";

        harness.send201(vertx, "LOG-STATION-201", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "LOG-STATION-201", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(2).size()).isEqualTo(0);

                    assertThat(harness.logStatusSink.events()).hasSize(1);
                    FakeLogStatusSink.Event e = harness.logStatusSink.events().get(0);
                    assertThat(e.status()).isEqualTo(UploadLogStatus.UPLOADING);
                    assertThat(e.requestId()).isEqualTo(11);
                    ctx.completeNow();
                }));
    }

    @Test
    void log_status_idle_without_request_id(Vertx vertx, VertxTestContext ctx) {
        // N01.FR.13 — requestId optional when no upload is ongoing
        String msg = """
                [2,"lsn-2","LogStatusNotification",{
                  "status": "Idle"
                }]""";

        harness.send201(vertx, "LOG-STATION-202", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "LOG-STATION-202", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(harness.logStatusSink.events()).hasSize(1);
                    FakeLogStatusSink.Event e = harness.logStatusSink.events().get(0);
                    assertThat(e.status()).isEqualTo(UploadLogStatus.IDLE);
                    assertThat(e.requestId()).isNull();
                    ctx.completeNow();
                }));
    }

    @Test
    void notify_event_single_alerting_event_routed_to_sink(Vertx vertx, VertxTestContext ctx) {
        String msg = """
                [2,"ne-1","NotifyEvent",{
                  "generatedAt": "2027-04-01T10:00:00Z",
                  "seqNo": 0,
                  "eventData": [{
                    "eventId": 1001,
                    "timestamp": "2027-04-01T10:00:00Z",
                    "trigger": "Alerting",
                    "actualValue": "32.5",
                    "eventNotificationType": "HardWiredMonitor",
                    "component": {"name": "Connector", "evse": {"id": 1, "connectorId": 1}},
                    "variable": {"name": "Temperature"}
                  }]
                }]""";

        harness.send201(vertx, "EVT-STATION-201", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "EVT-STATION-201", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(2).size()).isEqualTo(0);

                    assertThat(harness.notifyEventSink.frames()).hasSize(1);
                    FakeNotifyEventSink.Frame f = harness.notifyEventSink.frames().get(0);
                    assertThat(f.seqNo()).isEqualTo(0);
                    assertThat(f.tbc()).isFalse();
                    assertThat(f.eventData()).hasSize(1);

                    var e = f.eventData().get(0);
                    assertThat(e.eventId()).isEqualTo(1001);
                    assertThat(e.trigger()).isEqualTo(EventTrigger.ALERTING);
                    assertThat(e.actualValue()).isEqualTo("32.5");
                    assertThat(e.eventNotificationType())
                            .isEqualTo(EventNotificationType.HARD_WIRED_MONITOR);
                    assertThat(e.component().name()).isEqualTo("Connector");
                    assertThat(e.component().evse().id()).isEqualTo(1);
                    assertThat(e.component().evse().connectorId()).isEqualTo(1);
                    assertThat(e.variable().name()).isEqualTo("Temperature");
                    ctx.completeNow();
                }));
    }

    @Test
    void notify_event_multi_event_with_optional_fields_and_tbc(Vertx vertx, VertxTestContext ctx) {
        // Exercises: tbc=true, multiple events in one frame, and the optional
        // cause/cleared/transactionId/variableMonitoringId/techCode/techInfo fields.
        String msg = """
                [2,"ne-2","NotifyEvent",{
                  "generatedAt": "2027-04-01T11:00:00Z",
                  "seqNo": 0,
                  "tbc": true,
                  "eventData": [
                    {
                      "eventId": 2001,
                      "timestamp": "2027-04-01T11:00:00Z",
                      "trigger": "Periodic",
                      "actualValue": "ok",
                      "eventNotificationType": "CustomMonitor",
                      "variableMonitoringId": 7,
                      "techCode": "T-100",
                      "techInfo": "phase-A nominal",
                      "transactionId": "tx-9999",
                      "component": {"name": "AuthCtrlr"},
                      "variable": {"name": "Enabled"}
                    },
                    {
                      "eventId": 2002,
                      "timestamp": "2027-04-01T11:00:01Z",
                      "trigger": "Delta",
                      "cause": 2001,
                      "actualValue": "false",
                      "cleared": true,
                      "eventNotificationType": "PreconfiguredMonitor",
                      "component": {"name": "AuthCtrlr"},
                      "variable": {"name": "Enabled", "instance": "A"}
                    }
                  ]
                }]""";

        harness.send201(vertx, "EVT-STATION-202", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "EVT-STATION-202", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(harness.notifyEventSink.frames()).hasSize(1);
                    FakeNotifyEventSink.Frame f = harness.notifyEventSink.frames().get(0);
                    assertThat(f.tbc()).isTrue();
                    assertThat(f.eventData()).hasSize(2);

                    var first = f.eventData().get(0);
                    assertThat(first.trigger()).isEqualTo(EventTrigger.PERIODIC);
                    assertThat(first.eventNotificationType())
                            .isEqualTo(EventNotificationType.CUSTOM_MONITOR);
                    assertThat(first.variableMonitoringId()).isEqualTo(7);
                    assertThat(first.techCode()).isEqualTo("T-100");
                    assertThat(first.techInfo()).isEqualTo("phase-A nominal");
                    assertThat(first.transactionId()).isEqualTo("tx-9999");
                    assertThat(first.cause()).isNull();
                    assertThat(first.cleared()).isNull();

                    var second = f.eventData().get(1);
                    assertThat(second.trigger()).isEqualTo(EventTrigger.DELTA);
                    assertThat(second.cause()).isEqualTo(2001);
                    assertThat(second.cleared()).isTrue();
                    assertThat(second.variable().instance()).isEqualTo("A");
                    ctx.completeNow();
                }));
    }

    @Test
    void log_status_accepted_canceled_decoded(Vertx vertx, VertxTestContext ctx) {
        // N01.FR.12 — when a new GetLog interrupts an in-flight upload the station
        // reports AcceptedCanceled against the prior requestId.
        String msg = """
                [2,"lsn-3","LogStatusNotification",{
                  "status": "AcceptedCanceled",
                  "requestId": 99
                }]""";

        harness.send201(vertx, "LOG-STATION-203", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "LOG-STATION-203", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(harness.logStatusSink.events()).hasSize(1);
                    FakeLogStatusSink.Event e = harness.logStatusSink.events().get(0);
                    assertThat(e.status()).isEqualTo(UploadLogStatus.ACCEPTED_CANCELED);
                    assertThat(e.requestId()).isEqualTo(99);
                    ctx.completeNow();
                }));
    }
}
