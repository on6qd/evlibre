package com.evlibre.server.adapter.ocpp;

import com.evlibre.server.adapter.ocpp.testutil.OcppMessages;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.v201.diagnostics.UploadLogStatus;
import com.evlibre.server.test.fakes.FakeLogStatusSink;
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
