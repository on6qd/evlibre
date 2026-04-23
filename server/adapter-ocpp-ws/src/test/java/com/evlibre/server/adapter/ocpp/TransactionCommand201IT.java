package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.usecases.v201.GetTransactionStatusUseCaseV201;
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
 * Integration tests for CSMS-to-CS v2.0.1 Transaction-oriented (Block E)
 * commands that don't already have their own IT suite. Currently covers
 * {@code GetTransactionStatus} (E14). Payloads are schema-validated end-to-end
 * by {@code OcppSchemaValidator} in hard-reject mode.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class TransactionCommand201IT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("TX-STATION-201");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void get_transaction_status_ongoing_true_messages_false(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetTransactionStatus", payload -> Map.of(
                            "ongoingIndicator", true,
                            "messagesInQueue", false));
                    GetTransactionStatusUseCaseV201 useCase =
                            new GetTransactionStatusUseCaseV201(harness.commandSender201);
                    return useCase.getTransactionStatus(TENANT, STATION, "tx-live-001")
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isOngoing()).isTrue();
                                    assertThat(result.hasOngoingIndicator()).isTrue();
                                    assertThat(result.messagesInQueue()).isFalse();

                                    var cmd = client.receivedCommands("GetTransactionStatus").get(0);
                                    assertThat(cmd.payload().get("transactionId").asText())
                                            .isEqualTo("tx-live-001");
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
    void get_transaction_status_without_id_omits_ongoing_indicator(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetTransactionStatus", payload -> Map.of(
                            "messagesInQueue", true));
                    GetTransactionStatusUseCaseV201 useCase =
                            new GetTransactionStatusUseCaseV201(harness.commandSender201);
                    return useCase.getTransactionStatus(TENANT, STATION, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.hasOngoingIndicator()).isFalse();
                                    assertThat(result.messagesInQueue()).isTrue();

                                    var cmd = client.receivedCommands("GetTransactionStatus").get(0);
                                    assertThat(cmd.payload().has("transactionId")).isFalse();
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
    void get_transaction_status_unknown_transaction_reports_both_false(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetTransactionStatus", payload -> Map.of(
                            "ongoingIndicator", false,
                            "messagesInQueue", false));
                    GetTransactionStatusUseCaseV201 useCase =
                            new GetTransactionStatusUseCaseV201(harness.commandSender201);
                    return useCase.getTransactionStatus(TENANT, STATION, "tx-never-existed")
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isOngoing()).isFalse();
                                    assertThat(result.hasOngoingIndicator()).isTrue();
                                    assertThat(result.messagesInQueue()).isFalse();
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
