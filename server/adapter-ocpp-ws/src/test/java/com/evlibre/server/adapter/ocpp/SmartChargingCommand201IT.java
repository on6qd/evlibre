package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.ChargingProfileStatus;
import com.evlibre.server.core.domain.v201.dto.ClearChargingProfileStatus;
import com.evlibre.server.core.domain.v201.dto.GenericStatus;
import com.evlibre.server.core.domain.v201.dto.GetChargingProfilesStatus;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfile;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfileCriterion;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfileKind;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfilePurpose;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingRateUnit;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedule;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedulePeriod;
import com.evlibre.server.core.domain.v201.smartcharging.ClearChargingProfileCriterion;
import com.evlibre.server.core.usecases.v201.ClearChargingProfileUseCaseV201;
import com.evlibre.server.core.usecases.v201.GetChargingProfilesUseCaseV201;
import com.evlibre.server.core.usecases.v201.GetCompositeScheduleUseCaseV201;
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
    void clear_charging_profile_by_id_accepted(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("ClearChargingProfile", "Accepted");
                    ClearChargingProfileUseCaseV201 useCase =
                            new ClearChargingProfileUseCaseV201(harness.commandSender201);
                    return useCase.clearChargingProfile(TENANT, STATION, 101, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status()).isEqualTo(ClearChargingProfileStatus.ACCEPTED);

                                    var cmd = client.receivedCommands("ClearChargingProfile").get(0);
                                    assertThat(cmd.payload().get("chargingProfileId").asInt()).isEqualTo(101);
                                    assertThat(cmd.payload().has("chargingProfileCriteria")).isFalse();
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
    void clear_charging_profile_by_criterion_unknown(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("ClearChargingProfile", payload -> Map.of("status", "Unknown"));
                    ClearChargingProfileUseCaseV201 useCase =
                            new ClearChargingProfileUseCaseV201(harness.commandSender201);
                    ClearChargingProfileCriterion criterion = new ClearChargingProfileCriterion(
                            2, ChargingProfilePurpose.TX_DEFAULT_PROFILE, 1);
                    return useCase.clearChargingProfile(TENANT, STATION, null, criterion)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isFalse();
                                    assertThat(result.status()).isEqualTo(ClearChargingProfileStatus.UNKNOWN);

                                    var cmd = client.receivedCommands("ClearChargingProfile").get(0);
                                    assertThat(cmd.payload().has("chargingProfileId")).isFalse();
                                    var crit = cmd.payload().get("chargingProfileCriteria");
                                    assertThat(crit.get("evseId").asInt()).isEqualTo(2);
                                    assertThat(crit.get("chargingProfilePurpose").asText())
                                            .isEqualTo("TxDefaultProfile");
                                    assertThat(crit.get("stackLevel").asInt()).isEqualTo(1);
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

    @Test
    void get_composite_schedule_accepted_parses_schedule(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetCompositeSchedule", payload -> Map.of(
                            "status", "Accepted",
                            "schedule", Map.of(
                                    "evseId", 1,
                                    "duration", 3600,
                                    "scheduleStart", "2027-02-01T10:00:00Z",
                                    "chargingRateUnit", "W",
                                    "chargingSchedulePeriod", List.of(
                                            Map.of("startPeriod", 0, "limit", 22000.0),
                                            Map.of("startPeriod", 1800, "limit", 11000.0)))));
                    GetCompositeScheduleUseCaseV201 useCase =
                            new GetCompositeScheduleUseCaseV201(harness.commandSender201);
                    return useCase.getCompositeSchedule(TENANT, STATION, 1, 3600, ChargingRateUnit.WATTS)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status()).isEqualTo(GenericStatus.ACCEPTED);
                                    assertThat(result.schedule()).isNotNull();
                                    assertThat(result.schedule().evseId()).isEqualTo(1);
                                    assertThat(result.schedule().duration()).isEqualTo(3600);
                                    assertThat(result.schedule().chargingRateUnit())
                                            .isEqualTo(ChargingRateUnit.WATTS);
                                    assertThat(result.schedule().chargingSchedulePeriod()).hasSize(2);

                                    var cmd = client.receivedCommands("GetCompositeSchedule").get(0);
                                    assertThat(cmd.payload().get("duration").asInt()).isEqualTo(3600);
                                    assertThat(cmd.payload().get("evseId").asInt()).isEqualTo(1);
                                    assertThat(cmd.payload().get("chargingRateUnit").asText()).isEqualTo("W");
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
    void get_charging_profiles_accepted_sends_criterion(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetChargingProfiles", "Accepted");
                    GetChargingProfilesUseCaseV201 useCase =
                            new GetChargingProfilesUseCaseV201(harness.commandSender201);
                    ChargingProfileCriterion criterion = new ChargingProfileCriterion(
                            List.of(ChargingLimitSource.EMS, ChargingLimitSource.CSO),
                            null,
                            ChargingProfilePurpose.TX_DEFAULT_PROFILE,
                            1);
                    return useCase.getChargingProfiles(TENANT, STATION, 777, 3, criterion)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status()).isEqualTo(GetChargingProfilesStatus.ACCEPTED);

                                    var cmd = client.receivedCommands("GetChargingProfiles").get(0);
                                    assertThat(cmd.payload().get("requestId").asInt()).isEqualTo(777);
                                    assertThat(cmd.payload().get("evseId").asInt()).isEqualTo(3);

                                    var crit = cmd.payload().get("chargingProfile");
                                    assertThat(crit.get("chargingProfilePurpose").asText())
                                            .isEqualTo("TxDefaultProfile");
                                    assertThat(crit.get("stackLevel").asInt()).isEqualTo(1);
                                    var sources = crit.get("chargingLimitSource");
                                    assertThat(sources.isArray()).isTrue();
                                    assertThat(sources.get(0).asText()).isEqualTo("EMS");
                                    assertThat(sources.get(1).asText()).isEqualTo("CSO");
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
    void get_charging_profiles_no_profiles_status(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetChargingProfiles", "NoProfiles");
                    GetChargingProfilesUseCaseV201 useCase =
                            new GetChargingProfilesUseCaseV201(harness.commandSender201);
                    return useCase.getChargingProfiles(TENANT, STATION, 888, null, ChargingProfileCriterion.all())
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isFalse();
                                    assertThat(result.status()).isEqualTo(GetChargingProfilesStatus.NO_PROFILES);

                                    var cmd = client.receivedCommands("GetChargingProfiles").get(0);
                                    assertThat(cmd.payload().has("evseId")).isFalse();
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
    void get_composite_schedule_rejected_omits_schedule(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetCompositeSchedule", payload -> Map.of(
                            "status", "Rejected",
                            "statusInfo", Map.of("reasonCode", "UnknownEVSE")));
                    GetCompositeScheduleUseCaseV201 useCase =
                            new GetCompositeScheduleUseCaseV201(harness.commandSender201);
                    return useCase.getCompositeSchedule(TENANT, STATION, 99, 600, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isFalse();
                                    assertThat(result.schedule()).isNull();
                                    assertThat(result.statusInfoReason()).isEqualTo("UnknownEVSE");

                                    var cmd = client.receivedCommands("GetCompositeSchedule").get(0);
                                    assertThat(cmd.payload().has("chargingRateUnit")).isFalse();
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
