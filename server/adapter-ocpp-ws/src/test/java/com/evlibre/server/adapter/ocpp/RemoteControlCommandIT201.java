package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.RequestStartStopStatus;
import com.evlibre.server.core.domain.v201.model.IdToken;
import com.evlibre.server.core.domain.v201.model.IdTokenType;
import com.evlibre.server.core.usecases.v201.RequestStartTransactionUseCaseV201;
import com.evlibre.server.core.usecases.v201.RequestStopTransactionUseCaseV201;
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
 * Integration tests for CSMS-to-CS v2.0.1 Remote Control (Block F) commands
 * over a real WebSocket connection. All request/response payloads are schema-
 * validated end-to-end by OcppSchemaValidator (hard-reject mode).
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class RemoteControlCommandIT201 {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("RC-STATION-201");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void request_start_transaction_accepted_with_transaction_id_echo(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("RequestStartTransaction", payload -> Map.of(
                            "status", "Accepted",
                            "transactionId", "already-running-tx-99"));
                    RequestStartTransactionUseCaseV201 useCase =
                            new RequestStartTransactionUseCaseV201(harness.commandSender201);
                    return useCase.requestStartTransaction(TENANT, STATION, 42,
                                    IdToken.of("DRIVER-RFID-01", IdTokenType.ISO14443),
                                    2, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status()).isEqualTo(RequestStartStopStatus.ACCEPTED);
                                    assertThat(result.transactionId()).isEqualTo("already-running-tx-99");

                                    var cmd = client.receivedCommands("RequestStartTransaction").get(0);
                                    assertThat(cmd.payload().get("remoteStartId").asInt()).isEqualTo(42);
                                    assertThat(cmd.payload().get("evseId").asInt()).isEqualTo(2);
                                    assertThat(cmd.payload().has("groupIdToken")).isFalse();

                                    var idTokenNode = cmd.payload().get("idToken");
                                    assertThat(idTokenNode.get("idToken").asText()).isEqualTo("DRIVER-RFID-01");
                                    assertThat(idTokenNode.get("type").asText()).isEqualTo("ISO14443");
                                    assertThat(idTokenNode.has("additionalInfo")).isFalse();
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
    void request_start_transaction_with_group_id_token_serialised(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("RequestStartTransaction", "Accepted");
                    RequestStartTransactionUseCaseV201 useCase =
                            new RequestStartTransactionUseCaseV201(harness.commandSender201);
                    return useCase.requestStartTransaction(TENANT, STATION, 7,
                                    IdToken.of("driver-emaid", IdTokenType.EMAID),
                                    null,
                                    IdToken.of("fleet-acme", IdTokenType.CENTRAL))
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("RequestStartTransaction").get(0);
                                    assertThat(cmd.payload().has("evseId")).isFalse();
                                    var group = cmd.payload().get("groupIdToken");
                                    assertThat(group.get("idToken").asText()).isEqualTo("fleet-acme");
                                    assertThat(group.get("type").asText()).isEqualTo("Central");
                                    assertThat(cmd.payload().get("idToken").get("type").asText())
                                            .isEqualTo("eMAID");
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
    void request_start_transaction_rejected_surfaces_status_info(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("RequestStartTransaction", payload -> Map.of(
                            "status", "Rejected",
                            "statusInfo", Map.of("reasonCode", "UnknownEvse")));
                    RequestStartTransactionUseCaseV201 useCase =
                            new RequestStartTransactionUseCaseV201(harness.commandSender201);
                    return useCase.requestStartTransaction(TENANT, STATION, 1,
                                    IdToken.of("x", IdTokenType.CENTRAL), 99, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isFalse();
                                    assertThat(result.status()).isEqualTo(RequestStartStopStatus.REJECTED);
                                    assertThat(result.statusInfoReason()).isEqualTo("UnknownEvse");
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
    void request_start_transaction_on_v16_session_rejected_by_protocol_guard(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp1.6")
                .thenCompose(client -> {
                    RequestStartTransactionUseCaseV201 useCase =
                            new RequestStartTransactionUseCaseV201(harness.commandSender201);
                    return useCase.requestStartTransaction(TENANT, STATION, 1,
                                    IdToken.of("x", IdTokenType.CENTRAL), null, null)
                            .handle((result, err) -> {
                                ctx.verify(() -> {
                                    assertThat(err).isNotNull();
                                    assertThat(err.getCause())
                                            .isInstanceOf(IllegalStateException.class)
                                            .hasMessageContaining("Cannot send RequestStartTransaction via OCPP_201");
                                });
                                client.close();
                                return null;
                            });
                })
                .whenComplete((r, err) -> {
                    if (err != null) ctx.failNow(err);
                    else ctx.completeNow();
                });
    }

    @Test
    void request_stop_transaction_accepted_wire_shape(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("RequestStopTransaction", "Accepted");
                    RequestStopTransactionUseCaseV201 useCase =
                            new RequestStopTransactionUseCaseV201(harness.commandSender201);
                    return useCase.requestStopTransaction(TENANT, STATION, "tx-uuid-99")
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("RequestStopTransaction").get(0);
                                    assertThat(cmd.payload().get("transactionId").asText())
                                            .isEqualTo("tx-uuid-99");
                                    assertThat(cmd.payload().size()).isEqualTo(1);
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
    void request_stop_transaction_rejected_surfaces_status_info(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("RequestStopTransaction", payload -> Map.of(
                            "status", "Rejected",
                            "statusInfo", Map.of("reasonCode", "NoTransaction")));
                    RequestStopTransactionUseCaseV201 useCase =
                            new RequestStopTransactionUseCaseV201(harness.commandSender201);
                    return useCase.requestStopTransaction(TENANT, STATION, "not-a-real-tx")
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isFalse();
                                    assertThat(result.status()).isEqualTo(RequestStartStopStatus.REJECTED);
                                    assertThat(result.statusInfoReason()).isEqualTo("NoTransaction");
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
    void request_stop_transaction_on_v16_session_rejected_by_protocol_guard(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp1.6")
                .thenCompose(client -> {
                    RequestStopTransactionUseCaseV201 useCase =
                            new RequestStopTransactionUseCaseV201(harness.commandSender201);
                    return useCase.requestStopTransaction(TENANT, STATION, "tx")
                            .handle((result, err) -> {
                                ctx.verify(() -> {
                                    assertThat(err).isNotNull();
                                    assertThat(err.getCause())
                                            .isInstanceOf(IllegalStateException.class)
                                            .hasMessageContaining("Cannot send RequestStopTransaction via OCPP_201");
                                });
                                client.close();
                                return null;
                            });
                })
                .whenComplete((r, err) -> {
                    if (err != null) ctx.failNow(err);
                    else ctx.completeNow();
                });
    }
}
