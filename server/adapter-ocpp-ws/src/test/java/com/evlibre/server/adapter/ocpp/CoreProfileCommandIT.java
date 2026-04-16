package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.usecases.*;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CSMS-to-CS commands over a real WebSocket connection.
 * Uses OcppTestClient to simulate a charge station that auto-responds to commands.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class CoreProfileCommandIT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("CMD-STATION");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void reset_hard_accepted(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "CMD-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("Reset", "Accepted");
                    ResetStationUseCase useCase = new ResetStationUseCase(harness.commandSender);
                    return useCase.reset(TENANT, STATION, "Hard")
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(client.receivedCommands("Reset")).hasSize(1);
                                    var cmd = client.receivedCommands("Reset").get(0);
                                    assertThat(cmd.payload().get("type").asText()).isEqualTo("Hard");
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
    void reset_soft_accepted(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "CMD-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("Reset", "Accepted");
                    ResetStationUseCase useCase = new ResetStationUseCase(harness.commandSender);
                    return useCase.reset(TENANT, STATION, "Soft")
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("Reset").get(0);
                                    assertThat(cmd.payload().get("type").asText()).isEqualTo("Soft");
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
    void remote_start_transaction(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "CMD-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("RemoteStartTransaction", "Accepted");
                    RemoteStartTransactionUseCase useCase = new RemoteStartTransactionUseCase(harness.commandSender);
                    return useCase.remoteStart(TENANT, STATION, "TAG001", 1)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("RemoteStartTransaction").get(0);
                                    assertThat(cmd.payload().get("idTag").asText()).isEqualTo("TAG001");
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

    @Test
    void remote_stop_transaction(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "CMD-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("RemoteStopTransaction", "Accepted");
                    RemoteStopTransactionUseCase useCase = new RemoteStopTransactionUseCase(harness.commandSender);
                    return useCase.remoteStop(TENANT, STATION, 42)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("RemoteStopTransaction").get(0);
                                    assertThat(cmd.payload().get("transactionId").asInt()).isEqualTo(42);
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
    void change_availability_inoperative(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "CMD-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("ChangeAvailability", "Accepted");
                    ChangeAvailabilityUseCase useCase = new ChangeAvailabilityUseCase(harness.commandSender);
                    return useCase.changeAvailability(TENANT, STATION, 1, "Inoperative")
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("ChangeAvailability").get(0);
                                    assertThat(cmd.payload().get("type").asText()).isEqualTo("Inoperative");
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

    @Test
    void unlock_connector(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "CMD-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("UnlockConnector", "Unlocked");
                    UnlockConnectorUseCase useCase = new UnlockConnectorUseCase(harness.commandSender);
                    return useCase.unlockConnector(TENANT, STATION, 1)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.status()).isEqualTo("Unlocked");
                                    var cmd = client.receivedCommands("UnlockConnector").get(0);
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

    @Test
    void clear_cache(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "CMD-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("ClearCache", "Accepted");
                    ClearCacheUseCase useCase = new ClearCacheUseCase(harness.commandSender);
                    return useCase.clearCache(TENANT, STATION)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(client.receivedCommands("ClearCache")).hasSize(1);
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
    void data_transfer_unknown_vendor_returns_unknownVendor(Vertx vertx, VertxTestContext ctx) {
        // Default wiring has no registered vendorIds, so any value returns UnknownVendor.
        OcppTestClient.connect(vertx, harness, "CMD-STATION", "ocpp1.6")
                .thenCompose(client -> client.send(
                        com.evlibre.server.adapter.ocpp.testutil.OcppMessages
                                .dataTransfer16("com.unknown.vendor", "TestMsg", "hello")))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3); // CALLRESULT
                    assertThat(response.get(2).get("status").asText()).isEqualTo("UnknownVendor");
                    ctx.completeNow();
                }));
    }

    @Test
    void rejected_command_returns_rejected(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "CMD-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("Reset", "Rejected");
                    ResetStationUseCase useCase = new ResetStationUseCase(harness.commandSender);
                    return useCase.reset(TENANT, STATION, "Hard")
                            .thenApply(result -> {
                                ctx.verify(() -> assertThat(result.isAccepted()).isFalse());
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
