package com.evlibre.server.adapter.ocpp;

import com.evlibre.server.adapter.ocpp.testutil.OcppMessages;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.test.fakes.FakeSecurityEventSink;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the Phase 7 security inbound flows (block A03 —
 * SecurityEventNotification). Exercises wire→domain decoding plus dispatcher
 * registration through {@link OcppTestHarness}; payloads are schema-validated
 * by {@code OcppSchemaValidator} in hard-reject mode.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class Security201IT {

    private OcppTestHarness harness;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void security_event_with_tech_info_routed_to_sink(Vertx vertx, VertxTestContext ctx) {
        String msg = """
                [2,"sec-1","SecurityEventNotification",{
                  "type": "FirmwareUpdated",
                  "timestamp": "2027-05-01T10:00:00Z",
                  "techInfo": "fw=2.4.1 from CSMS"
                }]""";

        harness.send201(vertx, "SEC-STATION-201", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "SEC-STATION-201", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    // Response is empty per OCPP 2.0.1.
                    assertThat(resp.get(2).size()).isEqualTo(0);

                    assertThat(harness.securityEventSink.events()).hasSize(1);
                    FakeSecurityEventSink.Event captured = harness.securityEventSink.events().get(0);
                    assertThat(captured.event().type()).isEqualTo("FirmwareUpdated");
                    assertThat(captured.event().timestamp())
                            .isEqualTo(Instant.parse("2027-05-01T10:00:00Z"));
                    assertThat(captured.event().techInfo()).isEqualTo("fw=2.4.1 from CSMS");
                    ctx.completeNow();
                }));
    }

    @Test
    void security_event_without_tech_info(Vertx vertx, VertxTestContext ctx) {
        // techInfo is optional per the spec.
        String msg = """
                [2,"sec-2","SecurityEventNotification",{
                  "type": "Reset",
                  "timestamp": "2027-05-01T11:00:00Z"
                }]""";

        harness.send201(vertx, "SEC-STATION-202", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "SEC-STATION-202", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(harness.securityEventSink.events()).hasSize(1);
                    FakeSecurityEventSink.Event captured = harness.securityEventSink.events().get(0);
                    assertThat(captured.event().type()).isEqualTo("Reset");
                    assertThat(captured.event().techInfo()).isNull();
                    ctx.completeNow();
                }));
    }
}
