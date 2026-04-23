package com.evlibre.server.adapter.ocpp;

import com.evlibre.server.adapter.ocpp.testutil.OcppMessages;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.v201.firmware.FirmwareStatus;
import com.evlibre.server.core.domain.v201.firmware.PublishFirmwareStatus;
import com.evlibre.server.test.fakes.FakeFirmwareStatusSink;
import com.evlibre.server.test.fakes.FakePublishFirmwareStatusSink;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the Phase 6 firmware-status inbound flow (block L01).
 * Exercises wire→domain decoding plus dispatcher registration through
 * {@link OcppTestHarness}; payloads are schema-validated by
 * {@code OcppSchemaValidator} in hard-reject mode.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class Firmware201IT {

    private OcppTestHarness harness;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void firmware_status_with_request_id_routed_to_sink(Vertx vertx, VertxTestContext ctx) {
        String msg = """
                [2,"fsn-1","FirmwareStatusNotification",{
                  "status": "Downloaded",
                  "requestId": 42
                }]""";

        harness.send201(vertx, "FW-STATION-201", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "FW-STATION-201", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(2).size()).isEqualTo(0);

                    assertThat(harness.firmwareStatusSink.events()).hasSize(1);
                    FakeFirmwareStatusSink.Event e = harness.firmwareStatusSink.events().get(0);
                    assertThat(e.status()).isEqualTo(FirmwareStatus.DOWNLOADED);
                    assertThat(e.requestId()).isEqualTo(42);
                    ctx.completeNow();
                }));
    }

    @Test
    void firmware_status_idle_without_request_id(Vertx vertx, VertxTestContext ctx) {
        // L01.FR.20 — requestId is optional when status=Idle
        String msg = """
                [2,"fsn-2","FirmwareStatusNotification",{
                  "status": "Idle"
                }]""";

        harness.send201(vertx, "FW-STATION-202", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "FW-STATION-202", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(harness.firmwareStatusSink.events()).hasSize(1);
                    FakeFirmwareStatusSink.Event e = harness.firmwareStatusSink.events().get(0);
                    assertThat(e.status()).isEqualTo(FirmwareStatus.IDLE);
                    assertThat(e.requestId()).isNull();
                    ctx.completeNow();
                }));
    }

    @Test
    void publish_firmware_status_published_with_locations(Vertx vertx, VertxTestContext ctx) {
        // L03.FR.04 — Published status carries one URI per supported protocol
        String msg = """
                [2,"pfsn-1","PublishFirmwareStatusNotification",{
                  "status": "Published",
                  "requestId": 33,
                  "location": [
                    "http://controller.local/fw/v2.bin",
                    "ftp://controller.local/fw/v2.bin"
                  ]
                }]""";

        harness.send201(vertx, "PFW-STATION-201", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "PFW-STATION-201", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(2).size()).isEqualTo(0);

                    assertThat(harness.publishFirmwareStatusSink.events()).hasSize(1);
                    FakePublishFirmwareStatusSink.Event e =
                            harness.publishFirmwareStatusSink.events().get(0);
                    assertThat(e.status()).isEqualTo(PublishFirmwareStatus.PUBLISHED);
                    assertThat(e.requestId()).isEqualTo(33);
                    assertThat(e.locations()).containsExactly(
                            "http://controller.local/fw/v2.bin",
                            "ftp://controller.local/fw/v2.bin");
                    ctx.completeNow();
                }));
    }

    @Test
    void publish_firmware_status_invalid_checksum_no_locations(Vertx vertx, VertxTestContext ctx) {
        String msg = """
                [2,"pfsn-2","PublishFirmwareStatusNotification",{
                  "status": "InvalidChecksum",
                  "requestId": 44
                }]""";

        harness.send201(vertx, "PFW-STATION-202", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "PFW-STATION-202", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(harness.publishFirmwareStatusSink.events()).hasSize(1);
                    FakePublishFirmwareStatusSink.Event e =
                            harness.publishFirmwareStatusSink.events().get(0);
                    assertThat(e.status()).isEqualTo(PublishFirmwareStatus.INVALID_CHECKSUM);
                    assertThat(e.locations()).isEmpty();
                    ctx.completeNow();
                }));
    }

    @Test
    void firmware_status_install_rebooting_decoded(Vertx vertx, VertxTestContext ctx) {
        // Spot-check a multi-word PascalCase enum value to exercise the wire codec
        String msg = """
                [2,"fsn-3","FirmwareStatusNotification",{
                  "status": "InstallVerificationFailed",
                  "requestId": 7
                }]""";

        harness.send201(vertx, "FW-STATION-203", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "FW-STATION-203", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(harness.firmwareStatusSink.events()).hasSize(1);
                    FakeFirmwareStatusSink.Event e = harness.firmwareStatusSink.events().get(0);
                    assertThat(e.status()).isEqualTo(FirmwareStatus.INSTALL_VERIFICATION_FAILED);
                    assertThat(e.requestId()).isEqualTo(7);
                    ctx.completeNow();
                }));
    }
}
