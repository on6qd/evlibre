package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.test.fakes.FakeReservationRepository;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.usecases.CancelReservationUseCase;
import com.evlibre.server.core.usecases.ReserveNowUseCase;
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

@ExtendWith(VertxExtension.class)
@Tag("integration")
class ReservationProfileIT {

    private OcppTestHarness harness;
    private FakeReservationRepository reservationRepo;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("RES-STATION");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        reservationRepo = new FakeReservationRepository();
        harness.deploy(vertx, ctx);
    }

    @Test
    void reserveNow_accepted_and_saved(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "RES-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("ReserveNow", "Accepted");
                    var useCase = new ReserveNowUseCase(harness.commandSender, reservationRepo);
                    return useCase.reserveNow(TENANT, STATION, 1,
                                    Instant.parse("2025-06-01T12:00:00Z"), "TAG001")
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("ReserveNow").get(0);
                                    assertThat(cmd.payload().get("connectorId").asInt()).isEqualTo(1);
                                    assertThat(cmd.payload().get("idTag").asText()).isEqualTo("TAG001");
                                    assertThat(cmd.payload().has("reservationId")).isTrue();
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
    void cancelReservation_accepted(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "RES-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("ReserveNow", "Accepted");
                    client.onCommand("CancelReservation", "Accepted");
                    var reserveUseCase = new ReserveNowUseCase(harness.commandSender, reservationRepo);
                    var cancelUseCase = new CancelReservationUseCase(harness.commandSender, reservationRepo);

                    return reserveUseCase.reserveNow(TENANT, STATION, 1,
                                    Instant.parse("2025-06-01T12:00:00Z"), "TAG001")
                            .thenCompose(reserveResult -> {
                                int resId = (int) reserveResult.rawResponse().getOrDefault("reservationId",
                                        client.receivedCommands("ReserveNow").get(0).payload().get("reservationId").asInt());
                                // Use the reservationId from the command sent
                                int sentResId = client.receivedCommands("ReserveNow").get(0).payload().get("reservationId").asInt();
                                return cancelUseCase.cancelReservation(TENANT, STATION, sentResId);
                            })
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(client.receivedCommands("CancelReservation")).hasSize(1);
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
