package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.ChargingProfileStatus;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfile;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfileKind;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfilePurpose;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingRateUnit;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedule;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedulePeriod;
import com.evlibre.server.core.usecases.v201.SetChargingProfileUseCaseV201;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CSMS-to-CS v2.0.1 Smart Charging (Block K) commands
 * over a real WebSocket connection. All request/response payloads are schema-
 * validated end-to-end by OcppSchemaValidator (hard-reject mode).
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class SmartChargingCommand201IT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("SC-STATION-201");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    private static ChargingProfile relativeTxDefault(int id, int stack, double limitA) {
        return new ChargingProfile(
                id, stack,
                ChargingProfilePurpose.TX_DEFAULT_PROFILE,
                ChargingProfileKind.RELATIVE,
                null, null, null, null,
                List.of(new ChargingSchedule(1, null, 3600, ChargingRateUnit.AMPERES,
                        List.of(ChargingSchedulePeriod.of(0, limitA)), null)));
    }

    @Test
    void set_charging_profile_accepted_wire_shape(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("SetChargingProfile", "Accepted");
                    SetChargingProfileUseCaseV201 useCase =
                            new SetChargingProfileUseCaseV201(harness.commandSender201);
                    return useCase.setChargingProfile(TENANT, STATION, 0, relativeTxDefault(101, 0, 16.0))
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status()).isEqualTo(ChargingProfileStatus.ACCEPTED);

                                    var cmd = client.receivedCommands("SetChargingProfile").get(0);
                                    assertThat(cmd.payload().get("evseId").asInt()).isEqualTo(0);

                                    var profile = cmd.payload().get("chargingProfile");
                                    assertThat(profile.get("id").asInt()).isEqualTo(101);
                                    assertThat(profile.get("chargingProfilePurpose").asText())
                                            .isEqualTo("TxDefaultProfile");
                                    assertThat(profile.get("chargingProfileKind").asText())
                                            .isEqualTo("Relative");
                                    assertThat(profile.has("transactionId")).isFalse();

                                    var schedule = profile.get("chargingSchedule").get(0);
                                    assertThat(schedule.get("chargingRateUnit").asText()).isEqualTo("A");
                                    assertThat(schedule.get("duration").asInt()).isEqualTo(3600);

                                    var period = schedule.get("chargingSchedulePeriod").get(0);
                                    assertThat(period.get("startPeriod").asInt()).isEqualTo(0);
                                    assertThat(period.get("limit").asDouble()).isEqualTo(16.0);
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
    void set_tx_profile_carries_transaction_id_on_wire(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("SetChargingProfile", "Accepted");
                    ChargingProfile txProfile = new ChargingProfile(
                            5, 1,
                            ChargingProfilePurpose.TX_PROFILE,
                            ChargingProfileKind.RELATIVE,
                            null, null, null, "tx-abc-123",
                            List.of(new ChargingSchedule(1, null, null, ChargingRateUnit.WATTS,
                                    List.of(ChargingSchedulePeriod.of(0, 22000.0)), null)));
                    SetChargingProfileUseCaseV201 useCase =
                            new SetChargingProfileUseCaseV201(harness.commandSender201);
                    return useCase.setChargingProfile(TENANT, STATION, 2, txProfile)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();

                                    var cmd = client.receivedCommands("SetChargingProfile").get(0);
                                    assertThat(cmd.payload().get("evseId").asInt()).isEqualTo(2);
                                    var profile = cmd.payload().get("chargingProfile");
                                    assertThat(profile.get("chargingProfilePurpose").asText())
                                            .isEqualTo("TxProfile");
                                    assertThat(profile.get("transactionId").asText())
                                            .isEqualTo("tx-abc-123");
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
    void set_charging_profile_rejected_surfaces_reason_code(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("SetChargingProfile", payload -> Map.of(
                            "status", "Rejected",
                            "statusInfo", Map.of("reasonCode", "DuplicateProfile")));
                    SetChargingProfileUseCaseV201 useCase =
                            new SetChargingProfileUseCaseV201(harness.commandSender201);
                    return useCase.setChargingProfile(TENANT, STATION, 0, relativeTxDefault(202, 0, 32.0))
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isFalse();
                                    assertThat(result.status()).isEqualTo(ChargingProfileStatus.REJECTED);
                                    assertThat(result.statusInfoReason()).isEqualTo("DuplicateProfile");
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
