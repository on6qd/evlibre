package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GenericStatus;
import com.evlibre.server.core.domain.v201.firmware.Firmware;
import com.evlibre.server.core.domain.v201.firmware.UpdateFirmwareStatus;
import com.evlibre.server.core.usecases.v201.PublishFirmwareUseCaseV201;
import com.evlibre.server.core.usecases.v201.UpdateFirmwareUseCaseV201;
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
 * Integration tests for CSMS-to-CS v2.0.1 UpdateFirmware (block L01) over a
 * real WebSocket connection. Both directions are schema-validated end-to-end
 * by OcppSchemaValidator (hard-reject mode).
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class FirmwareCommand201IT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("FW-CMD-201");
    private static final Instant RETRIEVE = Instant.parse("2027-02-01T03:00:00Z");
    private static final Instant INSTALL = Instant.parse("2027-02-01T04:00:00Z");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void update_firmware_minimal_payload_accepted(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("UpdateFirmware", "Accepted");
                    UpdateFirmwareUseCaseV201 useCase =
                            new UpdateFirmwareUseCaseV201(harness.commandSender201);
                    return useCase.updateFirmware(TENANT, STATION, 17,
                                    Firmware.basic("https://csms.example.com/fw/v2.bin", RETRIEVE),
                                    null, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status()).isEqualTo(UpdateFirmwareStatus.ACCEPTED);

                                    var cmd = client.receivedCommands("UpdateFirmware").get(0);
                                    assertThat(cmd.payload().get("requestId").asInt()).isEqualTo(17);
                                    assertThat(cmd.payload().has("retries")).isFalse();
                                    assertThat(cmd.payload().has("retryInterval")).isFalse();

                                    var fw = cmd.payload().get("firmware");
                                    assertThat(fw.get("location").asText())
                                            .isEqualTo("https://csms.example.com/fw/v2.bin");
                                    assertThat(fw.get("retrieveDateTime").asText())
                                            .isEqualTo("2027-02-01T03:00:00Z");
                                    assertThat(fw.has("installDateTime")).isFalse();
                                    assertThat(fw.has("signingCertificate")).isFalse();
                                    assertThat(fw.has("signature")).isFalse();
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
    void update_firmware_full_payload_with_retries_and_signature(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("UpdateFirmware", "AcceptedCanceled");
                    UpdateFirmwareUseCaseV201 useCase =
                            new UpdateFirmwareUseCaseV201(harness.commandSender201);
                    Firmware firmware = new Firmware(
                            "https://csms.example.com/fw/v3.bin",
                            RETRIEVE, INSTALL,
                            "-----BEGIN CERTIFICATE-----\nMIIB...\n-----END CERTIFICATE-----",
                            "c2lnbmF0dXJlLWJ5dGVz");
                    return useCase.updateFirmware(TENANT, STATION, 99, firmware, 3, 60)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    // AcceptedCanceled is treated as accepted — station agreed to swap updates.
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status())
                                            .isEqualTo(UpdateFirmwareStatus.ACCEPTED_CANCELED);

                                    var cmd = client.receivedCommands("UpdateFirmware").get(0);
                                    assertThat(cmd.payload().get("retries").asInt()).isEqualTo(3);
                                    assertThat(cmd.payload().get("retryInterval").asInt()).isEqualTo(60);
                                    var fw = cmd.payload().get("firmware");
                                    assertThat(fw.get("installDateTime").asText())
                                            .isEqualTo("2027-02-01T04:00:00Z");
                                    assertThat(fw.get("signingCertificate").asText()).startsWith("-----BEGIN");
                                    assertThat(fw.get("signature").asText()).isEqualTo("c2lnbmF0dXJlLWJ5dGVz");
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
    void publish_firmware_minimal_payload_accepted(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("PublishFirmware", "Accepted");
                    PublishFirmwareUseCaseV201 useCase =
                            new PublishFirmwareUseCaseV201(harness.commandSender201);
                    return useCase.publishFirmware(TENANT, STATION, 51,
                                    "https://csms.example.com/fw/v5.bin",
                                    "0123456789abcdef0123456789abcdef",
                                    null, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status()).isEqualTo(GenericStatus.ACCEPTED);

                                    var cmd = client.receivedCommands("PublishFirmware").get(0);
                                    assertThat(cmd.payload().get("requestId").asInt()).isEqualTo(51);
                                    assertThat(cmd.payload().get("location").asText())
                                            .isEqualTo("https://csms.example.com/fw/v5.bin");
                                    assertThat(cmd.payload().get("checksum").asText())
                                            .isEqualTo("0123456789abcdef0123456789abcdef");
                                    assertThat(cmd.payload().has("retries")).isFalse();
                                    assertThat(cmd.payload().has("retryInterval")).isFalse();
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
    void publish_firmware_rejected_surfaces_reason(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("PublishFirmware", payload -> Map.of(
                            "status", "Rejected",
                            "statusInfo", Map.of("reasonCode", "DiskFull")));
                    PublishFirmwareUseCaseV201 useCase =
                            new PublishFirmwareUseCaseV201(harness.commandSender201);
                    return useCase.publishFirmware(TENANT, STATION, 52,
                                    "https://csms.example.com/fw/v6.bin",
                                    "abcdef0123456789abcdef0123456789",
                                    3, 60)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isFalse();
                                    assertThat(result.status()).isEqualTo(GenericStatus.REJECTED);
                                    assertThat(result.statusInfoReason()).isEqualTo("DiskFull");

                                    var cmd = client.receivedCommands("PublishFirmware").get(0);
                                    assertThat(cmd.payload().get("retries").asInt()).isEqualTo(3);
                                    assertThat(cmd.payload().get("retryInterval").asInt()).isEqualTo(60);
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
    void update_firmware_invalid_certificate_surfaces_reason(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("UpdateFirmware", payload -> Map.of(
                            "status", "InvalidCertificate",
                            "statusInfo", Map.of("reasonCode", "ChainNotTrusted")));
                    UpdateFirmwareUseCaseV201 useCase =
                            new UpdateFirmwareUseCaseV201(harness.commandSender201);
                    return useCase.updateFirmware(TENANT, STATION, 5,
                                    Firmware.basic("https://csms.example.com/fw/v4.bin", RETRIEVE),
                                    null, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isFalse();
                                    assertThat(result.status())
                                            .isEqualTo(UpdateFirmwareStatus.INVALID_CERTIFICATE);
                                    assertThat(result.statusInfoReason()).isEqualTo("ChainNotTrusted");
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
