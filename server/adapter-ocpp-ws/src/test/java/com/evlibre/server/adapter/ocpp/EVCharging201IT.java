package com.evlibre.server.adapter.ocpp;

import com.evlibre.server.adapter.ocpp.testutil.OcppMessages;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.v201.dto.GenericStatus;
import com.evlibre.server.core.domain.v201.dto.NotifyEVChargingNeedsStatus;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingRateUnit;
import com.evlibre.server.core.domain.v201.smartcharging.EnergyTransferMode;
import com.evlibre.server.test.fakes.FakeEVChargingSink;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wire-level end-to-end tests for the two ISO 15118 inbound messages —
 * {@code NotifyEVChargingNeeds} and {@code NotifyEVChargingSchedule}. Both
 * request payloads are schema-validated by {@code OcppSchemaValidator} in
 * hard-reject mode; both response payloads are authored by the handler and
 * validated on the way out.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class EVCharging201IT {

    private OcppTestHarness harness;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void notify_ev_charging_needs_dc_returns_accepted(Vertx vertx, VertxTestContext ctx) {
        String msg = """
                [2,"evn-1","NotifyEVChargingNeeds",{
                  "evseId": 2,
                  "maxScheduleTuples": 3,
                  "chargingNeeds": {
                    "requestedEnergyTransfer": "DC",
                    "departureTime": "2027-01-01T15:00:00Z",
                    "dcChargingParameters": {
                      "evMaxCurrent": 300,
                      "evMaxVoltage": 400,
                      "evMaxPower": 120000,
                      "stateOfCharge": 35,
                      "fullSoC": 95
                    }
                  }
                }]""";

        harness.evChargingSink.setNextNeedsStatus(NotifyEVChargingNeedsStatus.ACCEPTED);

        harness.send201(vertx, "EVN-STATION-201", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "EVN-STATION-201", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(0).asInt()).isEqualTo(3);
                    assertThat(resp.get(2).get("status").asText()).isEqualTo("Accepted");

                    assertThat(harness.evChargingSink.needsEvents()).hasSize(1);
                    FakeEVChargingSink.NeedsEvent e = harness.evChargingSink.needsEvents().get(0);
                    assertThat(e.evseId()).isEqualTo(2);
                    assertThat(e.maxScheduleTuples()).isEqualTo(3);
                    assertThat(e.chargingNeeds().requestedEnergyTransfer())
                            .isEqualTo(EnergyTransferMode.DC);
                    assertThat(e.chargingNeeds().departureTime()).isNotNull();
                    assertThat(e.chargingNeeds().dcChargingParameters()).isNotNull();
                    assertThat(e.chargingNeeds().dcChargingParameters().evMaxCurrent()).isEqualTo(300);
                    assertThat(e.chargingNeeds().dcChargingParameters().stateOfCharge()).isEqualTo(35);
                    assertThat(e.chargingNeeds().acChargingParameters()).isNull();
                    ctx.completeNow();
                }));
    }

    @Test
    void notify_ev_charging_needs_ac_three_phase_processing_status(Vertx vertx, VertxTestContext ctx) {
        String msg = """
                [2,"evn-2","NotifyEVChargingNeeds",{
                  "evseId": 1,
                  "chargingNeeds": {
                    "requestedEnergyTransfer": "AC_three_phase",
                    "acChargingParameters": {
                      "energyAmount": 22000,
                      "evMinCurrent": 6,
                      "evMaxCurrent": 32,
                      "evMaxVoltage": 400
                    }
                  }
                }]""";

        harness.evChargingSink.setNextNeedsStatus(NotifyEVChargingNeedsStatus.PROCESSING);

        harness.send201(vertx, "EVN-STATION-202", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "EVN-STATION-202", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(2).get("status").asText()).isEqualTo("Processing");

                    FakeEVChargingSink.NeedsEvent e = harness.evChargingSink.needsEvents().get(0);
                    assertThat(e.chargingNeeds().requestedEnergyTransfer())
                            .isEqualTo(EnergyTransferMode.AC_THREE_PHASE);
                    assertThat(e.chargingNeeds().acChargingParameters()).isNotNull();
                    assertThat(e.chargingNeeds().acChargingParameters().evMaxCurrent()).isEqualTo(32);
                    assertThat(e.maxScheduleTuples()).isNull();
                    ctx.completeNow();
                }));
    }

    @Test
    void notify_ev_charging_schedule_parses_and_acks(Vertx vertx, VertxTestContext ctx) {
        String msg = """
                [2,"evs-1","NotifyEVChargingSchedule",{
                  "timeBase": "2027-02-01T10:00:00Z",
                  "evseId": 2,
                  "chargingSchedule": {
                    "id": 88,
                    "chargingRateUnit": "W",
                    "chargingSchedulePeriod": [
                      {"startPeriod": 0, "limit": 22000.0},
                      {"startPeriod": 1800, "limit": 11000.0}
                    ]
                  }
                }]""";

        harness.evChargingSink.setNextScheduleStatus(GenericStatus.ACCEPTED);

        harness.send201(vertx, "EVS-STATION-201", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "EVS-STATION-201", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(2).get("status").asText()).isEqualTo("Accepted");

                    assertThat(harness.evChargingSink.scheduleEvents()).hasSize(1);
                    FakeEVChargingSink.ScheduleEvent e = harness.evChargingSink.scheduleEvents().get(0);
                    assertThat(e.evseId()).isEqualTo(2);
                    assertThat(e.timeBase()).isNotNull();
                    assertThat(e.chargingSchedule().id()).isEqualTo(88);
                    assertThat(e.chargingSchedule().chargingRateUnit()).isEqualTo(ChargingRateUnit.WATTS);
                    assertThat(e.chargingSchedule().chargingSchedulePeriod()).hasSize(2);
                    ctx.completeNow();
                }));
    }

    @Test
    void notify_ev_charging_schedule_rejected_surfaces_on_wire(Vertx vertx, VertxTestContext ctx) {
        String msg = """
                [2,"evs-2","NotifyEVChargingSchedule",{
                  "timeBase": "2027-02-01T10:00:00Z",
                  "evseId": 3,
                  "chargingSchedule": {
                    "id": 1,
                    "chargingRateUnit": "A",
                    "chargingSchedulePeriod": [{"startPeriod": 0, "limit": 16.0}]
                  }
                }]""";

        harness.evChargingSink.setNextScheduleStatus(GenericStatus.REJECTED);

        harness.send201(vertx, "EVS-STATION-202", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "EVS-STATION-202", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(2).get("status").asText()).isEqualTo("Rejected");
                    ctx.completeNow();
                }));
    }
}
