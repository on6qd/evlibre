package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.usecases.v16.TriggerMessageUseCase;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
@Tag("integration")
class TriggerMessageProfileIT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("TM-STATION");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void triggerMessage_heartbeat_accepted(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "TM-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("TriggerMessage", "Accepted");
                    var useCase = new TriggerMessageUseCase(harness.commandSender);
                    return useCase.triggerMessage(TENANT, STATION, "Heartbeat", null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("TriggerMessage").get(0);
                                    assertThat(cmd.payload().get("requestedMessage").asText())
                                            .isEqualTo("Heartbeat");
                                });
                                client.close();
                                return result;
                            });
                })
                .whenComplete((r, err) -> {
                    if (err != null) ctx.failNow(err);
                    else ctx.completeNow();
                });
    }

    @Test
    void triggerMessage_statusNotification_with_connector(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "TM-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("TriggerMessage", "Accepted");
                    var useCase = new TriggerMessageUseCase(harness.commandSender);
                    return useCase.triggerMessage(TENANT, STATION, "StatusNotification", 1)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("TriggerMessage").get(0);
                                    assertThat(cmd.payload().get("connectorId").asInt()).isEqualTo(1);
                                });
                                client.close();
                                return result;
                            });
                })
                .whenComplete((r, err) -> {
                    if (err != null) ctx.failNow(err);
                    else ctx.completeNow();
                });
    }
}
