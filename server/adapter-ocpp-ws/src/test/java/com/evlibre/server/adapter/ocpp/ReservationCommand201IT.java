package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.CancelReservationStatus;
import com.evlibre.server.core.domain.v201.dto.ReserveNowStatus;
import com.evlibre.server.core.domain.v201.model.ConnectorType;
import com.evlibre.server.core.domain.v201.model.IdToken;
import com.evlibre.server.core.domain.v201.model.IdTokenType;
import com.evlibre.server.core.usecases.v201.CancelReservationUseCaseV201;
import com.evlibre.server.core.usecases.v201.ReserveNowUseCaseV201;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CSMS-to-CS v2.0.1 Reservation (Block H) commands over a
 * real WebSocket connection. All request/response payloads are schema-validated
 * end-to-end by OcppSchemaValidator (hard-reject mode).
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class ReservationCommand201IT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("RES-STATION-201");
    private static final Instant EXPIRY = Instant.parse("2027-01-01T12:00:00Z");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void reserve_now_accepted_minimal_payload(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("ReserveNow", "Accepted");
                    ReserveNowUseCaseV201 useCase =
                            new ReserveNowUseCaseV201(harness.commandSender201);
                    return useCase.reserveNow(TENANT, STATION, 101, EXPIRY,
                                    IdToken.of("RFID-01", IdTokenType.ISO14443),
                                    null, null, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status()).isEqualTo(ReserveNowStatus.ACCEPTED);

                                    var cmd = client.receivedCommands("ReserveNow").get(0);
                                    assertThat(cmd.payload().get("id").asInt()).isEqualTo(101);
                                    assertThat(cmd.payload().get("expiryDateTime").asText())
                                            .isEqualTo("2027-01-01T12:00:00Z");
                                    assertThat(cmd.payload().has("evseId")).isFalse();
                                    assertThat(cmd.payload().has("connectorType")).isFalse();
                                    assertThat(cmd.payload().has("groupIdToken")).isFalse();

                                    var idTokenNode = cmd.payload().get("idToken");
                                    assertThat(idTokenNode.get("idToken").asText()).isEqualTo("RFID-01");
                                    assertThat(idTokenNode.get("type").asText()).isEqualTo("ISO14443");
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
    void reserve_now_full_payload_with_evse_connector_type_and_group_id(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("ReserveNow", "Accepted");
                    ReserveNowUseCaseV201 useCase =
                            new ReserveNowUseCaseV201(harness.commandSender201);
                    return useCase.reserveNow(TENANT, STATION, 202, EXPIRY,
                                    IdToken.of("driver-emaid", IdTokenType.EMAID),
                                    3, ConnectorType.CCCS2,
                                    IdToken.of("fleet-acme", IdTokenType.CENTRAL))
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();

                                    var cmd = client.receivedCommands("ReserveNow").get(0);
                                    assertThat(cmd.payload().get("evseId").asInt()).isEqualTo(3);
                                    assertThat(cmd.payload().get("connectorType").asText()).isEqualTo("cCCS2");

                                    var group = cmd.payload().get("groupIdToken");
                                    assertThat(group.get("idToken").asText()).isEqualTo("fleet-acme");
                                    assertThat(group.get("type").asText()).isEqualTo("Central");
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
    void reserve_now_occupied_with_status_info_reason(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("ReserveNow", payload -> Map.of(
                            "status", "Occupied",
                            "statusInfo", Map.of("reasonCode", "EvseInUse")));
                    ReserveNowUseCaseV201 useCase =
                            new ReserveNowUseCaseV201(harness.commandSender201);
                    return useCase.reserveNow(TENANT, STATION, 303, EXPIRY,
                                    IdToken.of("RFID-02", IdTokenType.ISO14443),
                                    1, null, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isFalse();
                                    assertThat(result.status()).isEqualTo(ReserveNowStatus.OCCUPIED);
                                    assertThat(result.statusInfoReason()).isEqualTo("EvseInUse");
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
    void cancel_reservation_accepted_wire_shape(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("CancelReservation", "Accepted");
                    CancelReservationUseCaseV201 useCase =
                            new CancelReservationUseCaseV201(harness.commandSender201);
                    return useCase.cancelReservation(TENANT, STATION, 404)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status()).isEqualTo(CancelReservationStatus.ACCEPTED);

                                    var cmd = client.receivedCommands("CancelReservation").get(0);
                                    assertThat(cmd.payload().get("reservationId").asInt()).isEqualTo(404);
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
    void cancel_reservation_rejected_surfaces_reason_code(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("CancelReservation", payload -> Map.of(
                            "status", "Rejected",
                            "statusInfo", Map.of("reasonCode", "NoSuchReservation")));
                    CancelReservationUseCaseV201 useCase =
                            new CancelReservationUseCaseV201(harness.commandSender201);
                    return useCase.cancelReservation(TENANT, STATION, 999)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isFalse();
                                    assertThat(result.status()).isEqualTo(CancelReservationStatus.REJECTED);
                                    assertThat(result.statusInfoReason()).isEqualTo("NoSuchReservation");
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
