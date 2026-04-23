package com.evlibre.server.adapter.ocpp;

import com.evlibre.server.adapter.ocpp.testutil.OcppMessages;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfileKind;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfilePurpose;
import com.evlibre.server.test.fakes.FakeReportChargingProfilesSink;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the inbound CS→CSMS {@code ReportChargingProfiles}
 * handler (K09 follow-up). Drives the wire-level flow through
 * {@link OcppTestHarness} so the request payload is schema-validated end-to-end
 * and the adapter's wire→domain decoding is exercised.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class ReportChargingProfiles201IT {

    private OcppTestHarness harness;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void report_charging_profiles_single_frame_empty_response(Vertx vertx, VertxTestContext ctx) {
        String reportMsg = """
                [2,"rcp-1","ReportChargingProfiles",{
                  "requestId": 55,
                  "chargingLimitSource": "CSO",
                  "evseId": 2,
                  "tbc": false,
                  "chargingProfile": [{
                    "id": 7,
                    "stackLevel": 1,
                    "chargingProfilePurpose": "TxDefaultProfile",
                    "chargingProfileKind": "Relative",
                    "chargingSchedule": [{
                      "id": 1,
                      "chargingRateUnit": "A",
                      "chargingSchedulePeriod": [{"startPeriod": 0, "limit": 16.0}]
                    }]
                  }]
                }]""";

        harness.send201(vertx, "RCP-STATION-201", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "RCP-STATION-201", reportMsg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(0).asInt()).isEqualTo(3);
                    assertThat(resp.get(2).size()).isEqualTo(0);

                    assertThat(harness.reportChargingProfilesSink.frames()).hasSize(1);
                    FakeReportChargingProfilesSink.Frame f =
                            harness.reportChargingProfilesSink.frames().get(0);
                    assertThat(f.requestId()).isEqualTo(55);
                    assertThat(f.evseId()).isEqualTo(2);
                    assertThat(f.source()).isEqualTo(ChargingLimitSource.CSO);
                    assertThat(f.tbc()).isFalse();
                    assertThat(f.profiles()).hasSize(1);
                    assertThat(f.profiles().get(0).id()).isEqualTo(7);
                    assertThat(f.profiles().get(0).chargingProfilePurpose())
                            .isEqualTo(ChargingProfilePurpose.TX_DEFAULT_PROFILE);
                    assertThat(f.profiles().get(0).chargingProfileKind())
                            .isEqualTo(ChargingProfileKind.RELATIVE);
                    ctx.completeNow();
                }));
    }

    @Test
    void report_charging_profiles_tbc_true_flagged(Vertx vertx, VertxTestContext ctx) {
        String reportMsg = """
                [2,"rcp-2","ReportChargingProfiles",{
                  "requestId": 99,
                  "chargingLimitSource": "EMS",
                  "evseId": 0,
                  "tbc": true,
                  "chargingProfile": [{
                    "id": 42,
                    "stackLevel": 0,
                    "chargingProfilePurpose": "ChargingStationMaxProfile",
                    "chargingProfileKind": "Relative",
                    "chargingSchedule": [{
                      "id": 1,
                      "chargingRateUnit": "W",
                      "chargingSchedulePeriod": [{"startPeriod": 0, "limit": 50000.0}]
                    }]
                  }]
                }]""";

        harness.send201(vertx, "RCP-STATION-202", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "RCP-STATION-202", reportMsg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(harness.reportChargingProfilesSink.frames()).hasSize(1);
                    FakeReportChargingProfilesSink.Frame f =
                            harness.reportChargingProfilesSink.frames().get(0);
                    assertThat(f.tbc()).isTrue();
                    assertThat(f.source()).isEqualTo(ChargingLimitSource.EMS);
                    assertThat(f.evseId()).isEqualTo(0);
                    ctx.completeNow();
                }));
    }
}
