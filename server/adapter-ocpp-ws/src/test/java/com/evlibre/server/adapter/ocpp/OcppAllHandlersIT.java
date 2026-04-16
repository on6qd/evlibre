package com.evlibre.server.adapter.ocpp;

import com.evlibre.server.adapter.ocpp.testutil.OcppMessages;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.fasterxml.jackson.databind.JsonNode;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
@Tag("integration")
class OcppAllHandlersIT {

    private OcppTestHarness harness;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    // --- OCPP 1.6 ---

    @Test
    void bootNotification_returns_accepted_v16(Vertx vertx, VertxTestContext ctx) {
        harness.send16(vertx, "CHARGER-001", OcppMessages.bootNotification16("boot-1", "ABB", "Terra AC"))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(1).asText()).isEqualTo("boot-1");
                    assertThat(response.get(2).get("status").asText()).isEqualTo("Accepted");
                    assertThat(response.get(2).get("interval").asInt()).isEqualTo(900);
                    assertThat(response.get(2).has("currentTime")).isTrue();
                    ctx.completeNow();
                }));
    }

    @Test
    void heartbeat_returns_current_time_v16(Vertx vertx, VertxTestContext ctx) {
        harness.send16(vertx, "CHARGER-001", OcppMessages.bootNotification16("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send16(vertx, "CHARGER-001", OcppMessages.heartbeat16("hb-1")))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(1).asText()).isEqualTo("hb-1");
                    assertThat(response.get(2).has("currentTime")).isTrue();
                    ctx.completeNow();
                }));
    }

    @Test
    void authorize_accepted_v16(Vertx vertx, VertxTestContext ctx) {
        harness.send16(vertx, "CHARGER-001", OcppMessages.bootNotification16("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send16(vertx, "CHARGER-001", OcppMessages.authorize16("auth-1", "TAG001")))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(1).asText()).isEqualTo("auth-1");
                    assertThat(response.get(2).get("idTagInfo").get("status").asText()).isEqualTo("Accepted");
                    ctx.completeNow();
                }));
    }

    @Test
    void authorize_unknown_tag_defaults_to_invalid_v16(Vertx vertx, VertxTestContext ctx) {
        harness.send16(vertx, "CHARGER-001", OcppMessages.bootNotification16("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send16(vertx, "CHARGER-001", OcppMessages.authorize16("auth-2", "UNKNOWN")))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(2).get("idTagInfo").get("status").asText()).isEqualTo("Invalid");
                    ctx.completeNow();
                }));
    }

    @Test
    void statusNotification_accepted_v16(Vertx vertx, VertxTestContext ctx) {
        harness.send16(vertx, "CHARGER-001", OcppMessages.bootNotification16("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send16(vertx, "CHARGER-001",
                        OcppMessages.statusNotification16("sn-1", 1, "Available", "NoError")))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(1).asText()).isEqualTo("sn-1");
                    ctx.completeNow();
                }));
    }

    @Test
    void startTransaction_returns_id_v16(Vertx vertx, VertxTestContext ctx) {
        harness.send16(vertx, "CHARGER-001", OcppMessages.bootNotification16("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send16(vertx, "CHARGER-001",
                        OcppMessages.startTransaction16("start-1", 1, "TAG001", 0, Instant.parse("2025-01-15T10:00:00Z"))))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(1).asText()).isEqualTo("start-1");
                    assertThat(response.get(2).get("transactionId").asInt()).isPositive();
                    assertThat(response.get(2).get("idTagInfo").get("status").asText()).isEqualTo("Accepted");
                    ctx.completeNow();
                }));
    }

    @Test
    void stopTransaction_accepted_v16(Vertx vertx, VertxTestContext ctx) {
        harness.send16(vertx, "CHARGER-001", OcppMessages.bootNotification16("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send16(vertx, "CHARGER-001",
                        OcppMessages.startTransaction16(1, "TAG001", 0, Instant.parse("2025-01-15T10:00:00Z"))))
                .thenCompose(startResp -> {
                    int txId = startResp.get(2).get("transactionId").asInt();
                    return harness.send16(vertx, "CHARGER-001",
                            OcppMessages.stopTransaction16("stop-1", txId, 5000, Instant.parse("2025-01-15T11:00:00Z"), "EVDisconnected"));
                })
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(1).asText()).isEqualTo("stop-1");
                    ctx.completeNow();
                }));
    }

    @Test
    void meterValues_accepted_v16(Vertx vertx, VertxTestContext ctx) {
        harness.send16(vertx, "CHARGER-001", OcppMessages.bootNotification16("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send16(vertx, "CHARGER-001",
                        OcppMessages.startTransaction16(1, "TAG001", 0, Instant.parse("2025-01-15T10:00:00Z"))))
                .thenCompose(startResp -> {
                    int txId = startResp.get(2).get("transactionId").asInt();
                    return harness.send16(vertx, "CHARGER-001",
                            OcppMessages.meterValues16("mv-1", 1, txId, "1500", Instant.parse("2025-01-15T10:30:00Z")));
                })
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(1).asText()).isEqualTo("mv-1");
                    ctx.completeNow();
                }));
    }

    // --- OCPP 2.0.1 ---

    @Test
    void bootNotification_returns_accepted_v201(Vertx vertx, VertxTestContext ctx) {
        harness.send201(vertx, "CHARGER-201", OcppMessages.bootNotification201("boot-201", "ABB", "Terra AC"))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(1).asText()).isEqualTo("boot-201");
                    assertThat(response.get(2).get("status").asText()).isEqualTo("Accepted");
                    assertThat(response.get(2).get("interval").asInt()).isEqualTo(900);
                    assertThat(response.get(2).has("currentTime")).isTrue();
                    ctx.completeNow();
                }));
    }

    @Test
    void heartbeat_returns_current_time_v201(Vertx vertx, VertxTestContext ctx) {
        harness.send201(vertx, "CHARGER-201", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "CHARGER-201", OcppMessages.heartbeat201("hb-201")))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(1).asText()).isEqualTo("hb-201");
                    assertThat(response.get(2).has("currentTime")).isTrue();
                    ctx.completeNow();
                }));
    }

    @Test
    void authorize_accepted_v201(Vertx vertx, VertxTestContext ctx) {
        harness.send201(vertx, "CHARGER-201", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "CHARGER-201",
                        OcppMessages.authorize201("auth-201", "TAG001", "ISO14443")))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(1).asText()).isEqualTo("auth-201");
                    assertThat(response.get(2).get("idTokenInfo").get("status").asText()).isEqualTo("Accepted");
                    ctx.completeNow();
                }));
    }

    @Test
    void statusNotification_accepted_v201(Vertx vertx, VertxTestContext ctx) {
        harness.send201(vertx, "CHARGER-201", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "CHARGER-201",
                        OcppMessages.statusNotification201("sn-201", 1, 1, "Available", Instant.parse("2025-01-15T10:00:00Z"))))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(1).asText()).isEqualTo("sn-201");
                    ctx.completeNow();
                }));
    }

    @Test
    void transactionEvent_accepted_v201(Vertx vertx, VertxTestContext ctx) {
        harness.send201(vertx, "CHARGER-201", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "CHARGER-201",
                        OcppMessages.transactionEvent201("te-201", "Started", "tx-001", "CablePluggedIn", 0, Instant.parse("2025-01-15T10:00:00Z"))))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(1).asText()).isEqualTo("te-201");
                    ctx.completeNow();
                }));
    }

    @Test
    void meterValues_accepted_v201(Vertx vertx, VertxTestContext ctx) {
        harness.send201(vertx, "CHARGER-201", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "CHARGER-201",
                        OcppMessages.meterValues201("mv-201", 1, "1500", Instant.parse("2025-01-15T10:30:00Z"))))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(1).asText()).isEqualTo("mv-201");
                    ctx.completeNow();
                }));
    }

    // --- Firmware Management Profile ---

    @Test
    void diagnosticsStatusNotification_returns_empty_response(Vertx vertx, VertxTestContext ctx) {
        harness.send16(vertx, "CHARGER-001", OcppMessages.bootNotification16("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send16(vertx, "CHARGER-001",
                        OcppMessages.diagnosticsStatusNotification16("Uploaded")))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(2).size()).isEqualTo(0); // empty response
                    ctx.completeNow();
                }));
    }

    @Test
    void firmwareStatusNotification_returns_empty_response(Vertx vertx, VertxTestContext ctx) {
        harness.send16(vertx, "CHARGER-001", OcppMessages.bootNotification16("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send16(vertx, "CHARGER-001",
                        OcppMessages.firmwareStatusNotification16("Installed")))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(2).size()).isEqualTo(0); // empty response
                    ctx.completeNow();
                }));
    }

    // --- Error handling ---

    @Test
    void unknown_action_returns_not_implemented(Vertx vertx, VertxTestContext ctx) {
        harness.send16(vertx, "CHARGER-001", OcppMessages.unknownAction("NonExistentAction"))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(4);
                    assertThat(response.get(2).asText()).isEqualTo("NotImplemented");
                    ctx.completeNow();
                }));
    }

    @Test
    void missing_required_field_returns_protocol_error(Vertx vertx, VertxTestContext ctx) {
        // OCPP 1.6 §3.2.5: missing required property → ProtocolError (not FormationViolation).
        harness.send16(vertx, "CHARGER-001", OcppMessages.malformedCall("bad-1"))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(4);
                    assertThat(response.get(2).asText()).isEqualTo("ProtocolError");
                    ctx.completeNow();
                }));
    }

    @Test
    void constraint_violation_returns_property_constraint_violation(Vertx vertx, VertxTestContext ctx) {
        // StatusNotification with status value not in the enum — schema 'enum' violation.
        String req = "[2,\"bad-enum\",\"StatusNotification\","
                + "{\"connectorId\":1,\"errorCode\":\"NoError\",\"status\":\"NotARealStatus\"}]";
        harness.send16(vertx, "CHARGER-001", req)
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(4);
                    assertThat(response.get(2).asText()).isEqualTo("PropertyConstraintViolation");
                    ctx.completeNow();
                }));
    }
}
