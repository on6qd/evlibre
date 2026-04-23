package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.dto.ChangeAvailabilityStatus;
import com.evlibre.server.core.domain.v201.dto.ClearCacheStatus;
import com.evlibre.server.core.domain.v201.dto.SendLocalListStatus;
import com.evlibre.server.core.domain.v201.model.AuthorizationData;
import com.evlibre.server.core.domain.v201.model.AuthorizationStatus;
import com.evlibre.server.core.domain.v201.model.IdToken;
import com.evlibre.server.core.domain.v201.model.IdTokenInfo;
import com.evlibre.server.core.domain.v201.model.IdTokenType;
import com.evlibre.server.core.domain.v201.dto.OperationalStatus;
import com.evlibre.server.core.domain.v201.model.UpdateType;
import com.evlibre.server.core.usecases.v201.ChangeAvailabilityUseCaseV201;
import com.evlibre.server.core.usecases.v201.ClearCacheUseCaseV201;
import com.evlibre.server.core.usecases.v201.GetLocalListVersionUseCaseV201;
import com.evlibre.server.core.usecases.v201.SendLocalListUseCaseV201;
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
 * Integration tests for CSMS-to-CS v2.0.1 Availability (Block G), ClearCache
 * (Block C, C11), and Local Authorization List (Block D) commands over a real
 * WebSocket connection. All request/response payloads are schema-validated
 * end-to-end by OcppSchemaValidator (hard-reject mode).
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class AvailabilityAndAuthCommand201IT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("AA-STATION-201");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void change_availability_whole_station_accepted(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("ChangeAvailability", "Accepted");
                    ChangeAvailabilityUseCaseV201 useCase =
                            new ChangeAvailabilityUseCaseV201(harness.commandSender201);
                    return useCase.changeAvailability(TENANT, STATION, OperationalStatus.INOPERATIVE, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status()).isEqualTo(ChangeAvailabilityStatus.ACCEPTED);
                                    var cmd = client.receivedCommands("ChangeAvailability").get(0);
                                    assertThat(cmd.payload().get("operationalStatus").asText())
                                            .isEqualTo("Inoperative");
                                    assertThat(cmd.payload().has("evse")).isFalse();
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
    void change_availability_connector_targeted_scheduled(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("ChangeAvailability", "Scheduled");
                    ChangeAvailabilityUseCaseV201 useCase =
                            new ChangeAvailabilityUseCaseV201(harness.commandSender201);
                    return useCase.changeAvailability(TENANT, STATION,
                                    OperationalStatus.OPERATIVE, Evse.of(1, 2))
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.status()).isEqualTo(ChangeAvailabilityStatus.SCHEDULED);
                                    assertThat(result.isScheduled()).isTrue();
                                    var cmd = client.receivedCommands("ChangeAvailability").get(0);
                                    assertThat(cmd.payload().get("operationalStatus").asText())
                                            .isEqualTo("Operative");
                                    var evseNode = cmd.payload().get("evse");
                                    assertThat(evseNode.get("id").asInt()).isEqualTo(1);
                                    assertThat(evseNode.get("connectorId").asInt()).isEqualTo(2);
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
    void change_availability_on_v16_session_rejected_by_protocol_guard(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp1.6")
                .thenCompose(client -> {
                    ChangeAvailabilityUseCaseV201 useCase =
                            new ChangeAvailabilityUseCaseV201(harness.commandSender201);
                    return useCase.changeAvailability(TENANT, STATION, OperationalStatus.OPERATIVE, null)
                            .handle((result, err) -> {
                                ctx.verify(() -> {
                                    assertThat(err).isNotNull();
                                    assertThat(err.getCause())
                                            .isInstanceOf(IllegalStateException.class)
                                            .hasMessageContaining("Cannot send ChangeAvailability via OCPP_201");
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
    void clear_cache_accepted_empty_payload(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("ClearCache", "Accepted");
                    ClearCacheUseCaseV201 useCase =
                            new ClearCacheUseCaseV201(harness.commandSender201);
                    return useCase.clearCache(TENANT, STATION)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.status()).isEqualTo(ClearCacheStatus.ACCEPTED);
                                    var cmd = client.receivedCommands("ClearCache").get(0);
                                    assertThat(cmd.payload().size()).isZero();
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
    void clear_cache_rejected_with_reason(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("ClearCache", payload -> Map.of(
                            "status", "Rejected",
                            "statusInfo", Map.of("reasonCode", "NotEnabled")));
                    ClearCacheUseCaseV201 useCase =
                            new ClearCacheUseCaseV201(harness.commandSender201);
                    return useCase.clearCache(TENANT, STATION)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.status()).isEqualTo(ClearCacheStatus.REJECTED);
                                    assertThat(result.statusInfoReason()).isEqualTo("NotEnabled");
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
    void get_local_list_version_returns_installed_version(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetLocalListVersion",
                            payload -> Map.of("versionNumber", 17));
                    GetLocalListVersionUseCaseV201 useCase =
                            new GetLocalListVersionUseCaseV201(harness.commandSender201);
                    return useCase.getLocalListVersion(TENANT, STATION)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.versionNumber()).isEqualTo(17);
                                    assertThat(result.hasLocalList()).isTrue();
                                    var cmd = client.receivedCommands("GetLocalListVersion").get(0);
                                    assertThat(cmd.payload().size()).isZero();
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
    void get_local_list_version_zero_signals_no_list(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetLocalListVersion",
                            payload -> Map.of("versionNumber", 0));
                    GetLocalListVersionUseCaseV201 useCase =
                            new GetLocalListVersionUseCaseV201(harness.commandSender201);
                    return useCase.getLocalListVersion(TENANT, STATION)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.versionNumber()).isZero();
                                    assertThat(result.hasLocalList()).isFalse();
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
    void send_local_list_full_replace_wire_shape(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("SendLocalList", "Accepted");
                    SendLocalListUseCaseV201 useCase =
                            new SendLocalListUseCaseV201(harness.commandSender201);
                    var entry = AuthorizationData.add(
                            IdToken.of("DRIVER-RFID-01", IdTokenType.ISO14443),
                            IdTokenInfo.of(AuthorizationStatus.ACCEPTED));
                    return useCase.sendLocalList(TENANT, STATION, 12, UpdateType.FULL, List.of(entry))
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.status()).isEqualTo(SendLocalListStatus.ACCEPTED);
                                    var cmd = client.receivedCommands("SendLocalList").get(0);
                                    assertThat(cmd.payload().get("versionNumber").asInt()).isEqualTo(12);
                                    assertThat(cmd.payload().get("updateType").asText()).isEqualTo("Full");
                                    var list = cmd.payload().get("localAuthorizationList");
                                    assertThat(list.isArray()).isTrue();
                                    assertThat(list.size()).isEqualTo(1);
                                    var data = list.get(0);
                                    assertThat(data.get("idToken").get("idToken").asText())
                                            .isEqualTo("DRIVER-RFID-01");
                                    assertThat(data.get("idToken").get("type").asText())
                                            .isEqualTo("ISO14443");
                                    assertThat(data.get("idTokenInfo").get("status").asText())
                                            .isEqualTo("Accepted");
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
    void send_local_list_differential_remove_entry_omits_id_token_info(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("SendLocalList", "Accepted");
                    SendLocalListUseCaseV201 useCase =
                            new SendLocalListUseCaseV201(harness.commandSender201);
                    var removal = AuthorizationData.remove(
                            IdToken.of("driver-gone", IdTokenType.ISO14443));
                    return useCase.sendLocalList(TENANT, STATION, 13,
                                    UpdateType.DIFFERENTIAL, List.of(removal))
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("SendLocalList").get(0);
                                    assertThat(cmd.payload().get("updateType").asText())
                                            .isEqualTo("Differential");
                                    var entry = cmd.payload().get("localAuthorizationList").get(0);
                                    assertThat(entry.has("idToken")).isTrue();
                                    assertThat(entry.has("idTokenInfo")).isFalse();
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
    void send_local_list_version_mismatch_propagated(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("SendLocalList", payload -> Map.of(
                            "status", "VersionMismatch",
                            "statusInfo", Map.of("reasonCode", "OutOfSequence")));
                    SendLocalListUseCaseV201 useCase =
                            new SendLocalListUseCaseV201(harness.commandSender201);
                    return useCase.sendLocalList(TENANT, STATION, 2,
                                    UpdateType.DIFFERENTIAL, List.of())
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.status()).isEqualTo(SendLocalListStatus.VERSION_MISMATCH);
                                    assertThat(result.statusInfoReason()).isEqualTo("OutOfSequence");
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
    void send_local_list_on_v16_session_rejected_by_protocol_guard(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp1.6")
                .thenCompose(client -> {
                    SendLocalListUseCaseV201 useCase =
                            new SendLocalListUseCaseV201(harness.commandSender201);
                    return useCase.sendLocalList(TENANT, STATION, 1, UpdateType.FULL, List.of())
                            .handle((result, err) -> {
                                ctx.verify(() -> {
                                    assertThat(err).isNotNull();
                                    assertThat(err.getCause())
                                            .isInstanceOf(IllegalStateException.class)
                                            .hasMessageContaining("Cannot send SendLocalList via OCPP_201");
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
