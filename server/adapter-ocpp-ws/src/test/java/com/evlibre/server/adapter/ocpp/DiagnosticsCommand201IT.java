package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.diagnostics.GetLogStatus;
import com.evlibre.server.core.domain.v201.diagnostics.LogParameters;
import com.evlibre.server.core.domain.v201.diagnostics.LogType;
import com.evlibre.server.core.usecases.v201.GetLogUseCaseV201;
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
 * Integration tests for CSMS-to-CS v2.0.1 diagnostics commands (block N01 —
 * GetLog) over a real WebSocket connection. Both directions are
 * schema-validated end-to-end by OcppSchemaValidator (hard-reject mode).
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class DiagnosticsCommand201IT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("DIAG-CMD-201");
    private static final Instant OLDEST = Instant.parse("2027-03-01T00:00:00Z");
    private static final Instant LATEST = Instant.parse("2027-03-02T00:00:00Z");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void get_log_minimal_diagnostics_log_accepted(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetLog", payload -> Map.of(
                            "status", "Accepted",
                            "filename", "diag-2027-03-01.zip"));
                    GetLogUseCaseV201 useCase = new GetLogUseCaseV201(harness.commandSender201);
                    return useCase.getLog(TENANT, STATION, LogType.DIAGNOSTICS_LOG, 21,
                                    LogParameters.of("https://csms.example.com/uploads/log.zip"),
                                    null, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status()).isEqualTo(GetLogStatus.ACCEPTED);
                                    assertThat(result.filename()).isEqualTo("diag-2027-03-01.zip");

                                    var cmd = client.receivedCommands("GetLog").get(0);
                                    assertThat(cmd.payload().get("logType").asText()).isEqualTo("DiagnosticsLog");
                                    assertThat(cmd.payload().get("requestId").asInt()).isEqualTo(21);
                                    assertThat(cmd.payload().has("retries")).isFalse();
                                    assertThat(cmd.payload().has("retryInterval")).isFalse();

                                    var logNode = cmd.payload().get("log");
                                    assertThat(logNode.get("remoteLocation").asText())
                                            .isEqualTo("https://csms.example.com/uploads/log.zip");
                                    assertThat(logNode.has("oldestTimestamp")).isFalse();
                                    assertThat(logNode.has("latestTimestamp")).isFalse();
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
    void get_log_security_log_with_time_window_and_retries(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetLog", "AcceptedCanceled");
                    GetLogUseCaseV201 useCase = new GetLogUseCaseV201(harness.commandSender201);
                    return useCase.getLog(TENANT, STATION, LogType.SECURITY_LOG, 22,
                                    new LogParameters("ftp://logs.example.com/sec.log", OLDEST, LATEST),
                                    5, 30)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    // AcceptedCanceled is treated as accepted — N01.FR.12.
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status())
                                            .isEqualTo(GetLogStatus.ACCEPTED_CANCELED);
                                    assertThat(result.filename()).isNull();

                                    var cmd = client.receivedCommands("GetLog").get(0);
                                    assertThat(cmd.payload().get("logType").asText()).isEqualTo("SecurityLog");
                                    assertThat(cmd.payload().get("retries").asInt()).isEqualTo(5);
                                    assertThat(cmd.payload().get("retryInterval").asInt()).isEqualTo(30);
                                    var logNode = cmd.payload().get("log");
                                    assertThat(logNode.get("oldestTimestamp").asText())
                                            .isEqualTo("2027-03-01T00:00:00Z");
                                    assertThat(logNode.get("latestTimestamp").asText())
                                            .isEqualTo("2027-03-02T00:00:00Z");
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
    void get_log_rejected_surfaces_reason(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetLog", payload -> Map.of(
                            "status", "Rejected",
                            "statusInfo", Map.of("reasonCode", "UploadInProgress")));
                    GetLogUseCaseV201 useCase = new GetLogUseCaseV201(harness.commandSender201);
                    return useCase.getLog(TENANT, STATION, LogType.DIAGNOSTICS_LOG, 23,
                                    LogParameters.of("https://csms.example.com/uploads/x.log"),
                                    null, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isFalse();
                                    assertThat(result.status()).isEqualTo(GetLogStatus.REJECTED);
                                    assertThat(result.filename()).isNull();
                                    assertThat(result.statusInfoReason()).isEqualTo("UploadInProgress");
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
