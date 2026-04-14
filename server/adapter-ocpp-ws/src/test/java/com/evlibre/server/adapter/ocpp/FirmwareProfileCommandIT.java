package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.usecases.GetDiagnosticsUseCase;
import com.evlibre.server.core.usecases.UpdateFirmwareUseCase;
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
class FirmwareProfileCommandIT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("FW-STATION");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void getDiagnostics_receives_fileName(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "FW-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("GetDiagnostics", payload ->
                            Map.of("fileName", "diag-20250601.tgz"));
                    var useCase = new GetDiagnosticsUseCase(harness.commandSender);
                    return useCase.getDiagnostics(TENANT, STATION,
                                    "ftp://example.com/diag", null, null, null, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("GetDiagnostics").get(0);
                                    assertThat(cmd.payload().get("location").asText())
                                            .isEqualTo("ftp://example.com/diag");
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
    void updateFirmware_acknowledged(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "FW-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("UpdateFirmware", payload -> Map.of());
                    var useCase = new UpdateFirmwareUseCase(harness.commandSender);
                    return useCase.updateFirmware(TENANT, STATION,
                                    "ftp://example.com/fw.bin", "2025-06-01T00:00:00Z", 3, 120)
                            .thenApply(v -> {
                                ctx.verify(() -> {
                                    var cmd = client.receivedCommands("UpdateFirmware").get(0);
                                    assertThat(cmd.payload().get("location").asText())
                                            .isEqualTo("ftp://example.com/fw.bin");
                                    assertThat(cmd.payload().get("retrieveDate").asText())
                                            .isEqualTo("2025-06-01T00:00:00Z");
                                    assertThat(cmd.payload().get("retries").asInt()).isEqualTo(3);
                                });
                                client.close();
                                return v;
                            });
                })
                .whenComplete((r, err) -> {
                    if (err != null) ctx.failNow(err);
                    else ctx.completeNow();
                });
    }

    @Test
    void diagnosticsStatusNotification_via_testClient(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "FW-STATION", "ocpp1.6")
                .thenCompose(client -> client.send(
                        com.evlibre.server.adapter.ocpp.testutil.OcppMessages
                                .diagnosticsStatusNotification16("Uploading")))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(2).size()).isEqualTo(0);
                    ctx.completeNow();
                }));
    }

    @Test
    void firmwareStatusNotification_via_testClient(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "FW-STATION", "ocpp1.6")
                .thenCompose(client -> client.send(
                        com.evlibre.server.adapter.ocpp.testutil.OcppMessages
                                .firmwareStatusNotification16("Downloading")))
                .whenComplete((response, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(response.get(0).asInt()).isEqualTo(3);
                    assertThat(response.get(2).size()).isEqualTo(0);
                    ctx.completeNow();
                }));
    }
}
