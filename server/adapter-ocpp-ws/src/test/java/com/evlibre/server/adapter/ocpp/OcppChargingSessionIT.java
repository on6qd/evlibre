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

@ExtendWith(VertxExtension.class)
@Tag("integration")
class OcppChargingSessionIT {

    private OcppTestHarness harness;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void full_charging_session_v16(Vertx vertx, VertxTestContext ctx) {
        harness.send16(vertx, "CHARGER-E2E", OcppMessages.bootNotification16("ABB", "Terra AC"))
                .thenCompose(bootResp -> {
                    assertThat(bootResp.get(2).get("status").asText()).isEqualTo("Accepted");
                    return harness.send16(vertx, "CHARGER-E2E", OcppMessages.authorize16("TAG001"));
                })
                .thenCompose(authResp -> {
                    assertThat(authResp.get(2).get("idTagInfo").get("status").asText()).isEqualTo("Accepted");
                    return harness.send16(vertx, "CHARGER-E2E",
                            OcppMessages.startTransaction16(1, "TAG001", 0, Instant.parse("2025-01-15T10:00:00Z")));
                })
                .thenCompose(startResp -> {
                    assertThat(startResp.get(0).asInt()).isEqualTo(3);
                    int txId = startResp.get(2).get("transactionId").asInt();
                    assertThat(txId).isPositive();
                    assertThat(startResp.get(2).get("idTagInfo").get("status").asText()).isEqualTo("Accepted");
                    return harness.send16(vertx, "CHARGER-E2E",
                            OcppMessages.meterValues16(1, txId, "1500", Instant.parse("2025-01-15T10:30:00Z")))
                            .thenCompose(mvResp -> {
                                assertThat(mvResp.get(0).asInt()).isEqualTo(3);
                                return harness.send16(vertx, "CHARGER-E2E",
                                        OcppMessages.stopTransaction16(txId, 5000, Instant.parse("2025-01-15T11:00:00Z"), "EVDisconnected"));
                            });
                })
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    // Verify station was registered
                    assertThat(harness.stationRepo.count()).isEqualTo(1);
                    ctx.completeNow();
                }));
    }

    @Test
    void full_charging_session_v201(Vertx vertx, VertxTestContext ctx) {
        String txId = "tx-201-e2e";

        harness.send201(vertx, "CHARGER-E2E-201", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(bootResp -> {
                    assertThat(bootResp.get(2).get("status").asText()).isEqualTo("Accepted");
                    return harness.send201(vertx, "CHARGER-E2E-201",
                            OcppMessages.transactionEvent201("Started", txId, "CablePluggedIn", 0, Instant.parse("2025-01-15T10:00:00Z")));
                })
                .thenCompose(startResp -> {
                    assertThat(startResp.get(0).asInt()).isEqualTo(3);
                    return harness.send201(vertx, "CHARGER-E2E-201",
                            OcppMessages.transactionEvent201("Updated", txId, "MeterValuePeriodic", 1, Instant.parse("2025-01-15T10:30:00Z")));
                })
                .thenCompose(updateResp -> {
                    assertThat(updateResp.get(0).asInt()).isEqualTo(3);
                    return harness.send201(vertx, "CHARGER-E2E-201",
                            OcppMessages.transactionEvent201("Ended", txId, "EVCommunicationLost", 2, Instant.parse("2025-01-15T11:00:00Z")));
                })
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    // Verify events were logged
                    assertThat(harness.eventLog.entries()).hasSizeGreaterThanOrEqualTo(3);
                    ctx.completeNow();
                }));
    }
}
