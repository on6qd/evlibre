package com.evlibre.server.adapter.ocpp;

import com.evlibre.server.adapter.ocpp.testutil.OcppMessages;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
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
 * OCPP 2.0.1-only end-to-end integration tests. Dedicated class (separate from
 * {@code OcppAllHandlersIT}) so the v2.0.1 path has a clear home and regressions
 * are caught without having to scan a mixed test file.
 *
 * <p>Every test here exercises a v2.0.1 handler that is wired to a v201 use case
 * sibling — the separation rule under OCPP201 phase 0.2b means nothing here may
 * touch a v16 use case.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class Ocpp201HandlersIT {

    private OcppTestHarness harness;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void boot_then_heartbeat_then_authorize(Vertx vertx, VertxTestContext ctx) {
        harness.send201(vertx, "CS-201-1", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> {
                    ctx.verify(() -> {
                        assertThat(boot.get(2).get("status").asText()).isEqualTo("Accepted");
                        assertThat(harness.stationRepo.count()).isEqualTo(1);
                    });
                    return harness.send201(vertx, "CS-201-1", OcppMessages.heartbeat201());
                })
                .thenCompose(hb -> {
                    ctx.verify(() -> assertThat(hb.get(2).has("currentTime")).isTrue());
                    return harness.send201(vertx, "CS-201-1",
                            OcppMessages.authorize201("TAG001", "ISO14443"));
                })
                .whenComplete((auth, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(auth.get(2).get("idTokenInfo").get("status").asText())
                            .isEqualTo("Accepted");
                    ctx.completeNow();
                }));
    }

    @Test
    void statusNotification_accepted(Vertx vertx, VertxTestContext ctx) {
        harness.send201(vertx, "CS-201-2", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "CS-201-2",
                        OcppMessages.statusNotification201(1, 1, "Available",
                                Instant.parse("2026-04-21T10:00:00Z"))))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(0).asInt()).isEqualTo(3);
                    ctx.completeNow();
                }));
    }

    @Test
    void transactionEvent_accepted(Vertx vertx, VertxTestContext ctx) {
        harness.send201(vertx, "CS-201-3", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "CS-201-3",
                        OcppMessages.transactionEvent201("Started", "tx-e2e", "CablePluggedIn", 0,
                                Instant.parse("2026-04-21T10:00:00Z"))))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(0).asInt()).isEqualTo(3);
                    ctx.completeNow();
                }));
    }

    @Test
    void meterValues_accepted(Vertx vertx, VertxTestContext ctx) {
        harness.send201(vertx, "CS-201-4", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "CS-201-4",
                        OcppMessages.meterValues201(1, "1500", Instant.parse("2026-04-21T10:00:00Z"))))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(0).asInt()).isEqualTo(3);
                    ctx.completeNow();
                }));
    }

    @Test
    void notifyReport_accepted_and_empty_response(Vertx vertx, VertxTestContext ctx) {
        // Proves NotifyReportHandler201 is now registered (it was dead code prior to 0.4a).
        harness.send201(vertx, "CS-201-5", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "CS-201-5",
                        OcppMessages.notifyReport201(1, 0, Instant.parse("2026-04-21T10:00:00Z"),
                                "SecurityCtrlr", "BasicAuthPassword")))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(0).asInt()).isEqualTo(3);
                    // Empty object response
                    assertThat(resp.get(2).size()).isEqualTo(0);
                    ctx.completeNow();
                }));
    }

    @Test
    void v16_action_sent_on_v201_session_returns_not_implemented(Vertx vertx, VertxTestContext ctx) {
        // Protocol separation contract: StartTransaction is a v1.6-only action. Dispatching
        // it over a v2.0.1-negotiated session must not resolve to the v16 handler — the
        // dispatcher is keyed on protocol:action and should return NotImplemented.
        String startTransaction16 = "[2,\"cross-proto\",\"StartTransaction\","
                + "{\"connectorId\":1,\"idTag\":\"TAG001\",\"meterStart\":0,"
                + "\"timestamp\":\"2026-04-21T10:00:00Z\"}]";

        harness.send201(vertx, "CS-201-6", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "CS-201-6", startTransaction16))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(0).asInt()).isEqualTo(4); // CALLERROR
                    assertThat(resp.get(2).asText()).isEqualTo("NotImplemented");
                    ctx.completeNow();
                }));
    }

    @Test
    void v201_action_sent_on_v16_session_returns_not_implemented(Vertx vertx, VertxTestContext ctx) {
        // Mirror of the above: TransactionEvent is v2.0.1-only. A v1.6-negotiated session
        // must not be able to reach the v201 handler.
        String transactionEvent201 = "[2,\"cross-proto-rev\",\"TransactionEvent\","
                + "{\"eventType\":\"Started\",\"timestamp\":\"2026-04-21T10:00:00Z\","
                + "\"triggerReason\":\"CablePluggedIn\",\"seqNo\":0,"
                + "\"transactionInfo\":{\"transactionId\":\"t\"}}]";

        harness.send16(vertx, "CS-16-cross", OcppMessages.bootNotification16("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send16(vertx, "CS-16-cross", transactionEvent201))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(0).asInt()).isEqualTo(4);
                    assertThat(resp.get(2).asText()).isEqualTo("NotImplemented");
                    ctx.completeNow();
                }));
    }
}
