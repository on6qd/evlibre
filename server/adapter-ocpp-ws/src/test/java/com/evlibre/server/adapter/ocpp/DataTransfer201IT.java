package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.DataTransferStatus;
import com.evlibre.server.core.usecases.v201.SendDataTransferUseCaseV201;
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
 * End-to-end integration tests for CSMS-to-CS v2.0.1 DataTransfer (Block P,
 * Use Case P01) over a real WebSocket connection with schema-validated
 * request/response payloads.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class DataTransfer201IT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("DT-STATION-201");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void data_transfer_accepted_minimal_payload(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("DataTransfer", payload -> Map.of("status", "Accepted"));
                    SendDataTransferUseCaseV201 useCase =
                            new SendDataTransferUseCaseV201(harness.commandSender201);
                    return useCase.sendDataTransfer(TENANT, STATION, "com.evlibre.probe", null, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("DataTransfer").get(0);
                                    assertThat(cmd.payload().get("vendorId").asText())
                                            .isEqualTo("com.evlibre.probe");
                                    assertThat(cmd.payload().has("messageId")).isFalse();
                                    assertThat(cmd.payload().has("data")).isFalse();
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
    void data_transfer_with_object_data_round_trips(Vertx vertx, VertxTestContext ctx) {
        // `data` is anyType; verify a structured object serializes outbound and the echoed
        // response payload makes it back into DataTransferResult.data().
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("DataTransfer", payload -> Map.of(
                            "status", "Accepted",
                            "data", Map.of("echo", "pong", "n", 7)));
                    SendDataTransferUseCaseV201 useCase =
                            new SendDataTransferUseCaseV201(harness.commandSender201);
                    return useCase.sendDataTransfer(TENANT, STATION,
                                    "com.evlibre.probe", "ping",
                                    Map.of("start_time", "2026-04-23T10:00:00Z"))
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.status()).isEqualTo(DataTransferStatus.ACCEPTED);
                                    assertThat(result.data()).isInstanceOf(Map.class);
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> echoed = (Map<String, Object>) result.data();
                                    assertThat(echoed).containsEntry("echo", "pong");

                                    var cmd = client.receivedCommands("DataTransfer").get(0);
                                    assertThat(cmd.payload().get("messageId").asText()).isEqualTo("ping");
                                    assertThat(cmd.payload().get("data").get("start_time").asText())
                                            .isEqualTo("2026-04-23T10:00:00Z");
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
    void data_transfer_rejected_with_statusInfo_reason(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("DataTransfer", payload -> Map.of(
                            "status", "Rejected",
                            "statusInfo", Map.of("reasonCode", "BadRequest")));
                    SendDataTransferUseCaseV201 useCase =
                            new SendDataTransferUseCaseV201(harness.commandSender201);
                    return useCase.sendDataTransfer(TENANT, STATION, "com.evlibre.probe", "ping", null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.status()).isEqualTo(DataTransferStatus.REJECTED);
                                    assertThat(result.statusInfoReason()).isEqualTo("BadRequest");
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
    void data_transfer_on_v16_session_rejected_by_protocol_guard(Vertx vertx, VertxTestContext ctx) {
        // Protocol guard: the v201 sender must refuse to dispatch over a v1.6-negotiated session.
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp1.6")
                .thenCompose(client -> {
                    SendDataTransferUseCaseV201 useCase =
                            new SendDataTransferUseCaseV201(harness.commandSender201);
                    return useCase.sendDataTransfer(TENANT, STATION, "com.evlibre.probe", null, null)
                            .handle((result, err) -> {
                                ctx.verify(() -> {
                                    assertThat(err).isNotNull();
                                    assertThat(err.getCause())
                                            .isInstanceOf(IllegalStateException.class)
                                            .hasMessageContaining("Cannot send DataTransfer via OCPP_201");
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
