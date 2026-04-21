package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.usecases.ClearChargingProfileUseCase;
import com.evlibre.server.core.usecases.GetCompositeScheduleUseCase;
import com.evlibre.server.core.usecases.SetChargingProfileUseCase;
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
class SmartChargingProfileIT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("SC-STATION");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void setChargingProfile_accepted(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "SC-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("SetChargingProfile", "Accepted");
                    var useCase = new SetChargingProfileUseCase(harness.commandSender);
                    Map<String, Object> profile = Map.of(
                            "chargingProfileId", 1,
                            "stackLevel", 0,
                            "chargingProfilePurpose", "TxDefaultProfile",
                            "chargingProfileKind", "Absolute");
                    return useCase.setChargingProfile(TENANT, STATION, 1, profile)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(client.receivedCommands("SetChargingProfile")).hasSize(1);
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
    void clearChargingProfile_accepted(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "SC-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("ClearChargingProfile", "Accepted");
                    var useCase = new ClearChargingProfileUseCase(harness.commandSender);
                    return useCase.clearChargingProfile(TENANT, STATION, null, 1, null, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("ClearChargingProfile").get(0);
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
    void getCompositeSchedule_accepted(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "SC-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("GetCompositeSchedule", "Accepted");
                    var useCase = new GetCompositeScheduleUseCase(harness.commandSender);
                    return useCase.getCompositeSchedule(TENANT, STATION, 1, 3600, "W")
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("GetCompositeSchedule").get(0);
                                    assertThat(cmd.payload().get("duration").asInt()).isEqualTo(3600);
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
