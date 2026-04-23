package com.evlibre.server.adapter.ocpp;

import com.evlibre.server.adapter.ocpp.testutil.OcppMessages;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingRateUnit;
import com.evlibre.server.test.fakes.FakeChargingLimitSink;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the two inbound charging-limit messages (K13/K14).
 * Exercises wire→domain decoding and dispatcher registration through
 * {@link OcppTestHarness}; payloads are schema-validated by
 * {@code OcppSchemaValidator} in hard-reject mode.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class ChargingLimit201IT {

    private OcppTestHarness harness;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void notify_charging_limit_with_schedule_routed_to_sink(Vertx vertx, VertxTestContext ctx) {
        String msg = """
                [2,"ncl-1","NotifyChargingLimit",{
                  "evseId": 2,
                  "chargingLimit": {
                    "chargingLimitSource": "SO",
                    "isGridCritical": true
                  },
                  "chargingSchedule": [{
                    "id": 1,
                    "chargingRateUnit": "W",
                    "chargingSchedulePeriod": [
                      {"startPeriod": 0, "limit": 22000.0},
                      {"startPeriod": 1800, "limit": 11000.0}
                    ]
                  }]
                }]""";

        harness.send201(vertx, "CL-STATION-201", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "CL-STATION-201", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(2).size()).isEqualTo(0);

                    assertThat(harness.chargingLimitSink.notifyEvents()).hasSize(1);
                    FakeChargingLimitSink.NotifyEvent n = harness.chargingLimitSink.notifyEvents().get(0);
                    assertThat(n.evseId()).isEqualTo(2);
                    assertThat(n.chargingLimit().chargingLimitSource()).isEqualTo(ChargingLimitSource.SO);
                    assertThat(n.chargingLimit().isGridCritical()).isTrue();
                    assertThat(n.schedules()).hasSize(1);
                    assertThat(n.schedules().get(0).chargingRateUnit()).isEqualTo(ChargingRateUnit.WATTS);
                    assertThat(n.schedules().get(0).chargingSchedulePeriod()).hasSize(2);
                    ctx.completeNow();
                }));
    }

    @Test
    void notify_charging_limit_without_schedule_or_grid_critical(Vertx vertx, VertxTestContext ctx) {
        String msg = """
                [2,"ncl-2","NotifyChargingLimit",{
                  "chargingLimit": {
                    "chargingLimitSource": "EMS"
                  }
                }]""";

        harness.send201(vertx, "CL-STATION-202", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "CL-STATION-202", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(harness.chargingLimitSink.notifyEvents()).hasSize(1);
                    FakeChargingLimitSink.NotifyEvent n = harness.chargingLimitSink.notifyEvents().get(0);
                    assertThat(n.evseId()).isNull();
                    assertThat(n.chargingLimit().chargingLimitSource()).isEqualTo(ChargingLimitSource.EMS);
                    assertThat(n.chargingLimit().isGridCritical()).isNull();
                    assertThat(n.schedules()).isEmpty();
                    ctx.completeNow();
                }));
    }

    @Test
    void cleared_charging_limit_station_wide_empty_response(Vertx vertx, VertxTestContext ctx) {
        String msg = """
                [2,"ccl-1","ClearedChargingLimit",{
                  "chargingLimitSource": "CSO"
                }]""";

        harness.send201(vertx, "CL-STATION-203", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "CL-STATION-203", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(resp.get(2).size()).isEqualTo(0);

                    assertThat(harness.chargingLimitSink.clearedEvents()).hasSize(1);
                    FakeChargingLimitSink.ClearedEvent c = harness.chargingLimitSink.clearedEvents().get(0);
                    assertThat(c.source()).isEqualTo(ChargingLimitSource.CSO);
                    assertThat(c.evseId()).isNull();
                    ctx.completeNow();
                }));
    }

    @Test
    void cleared_charging_limit_with_evse_id(Vertx vertx, VertxTestContext ctx) {
        String msg = """
                [2,"ccl-2","ClearedChargingLimit",{
                  "chargingLimitSource": "SO",
                  "evseId": 3
                }]""";

        harness.send201(vertx, "CL-STATION-204", OcppMessages.bootNotification201("ABB", "Terra AC"))
                .thenCompose(boot -> harness.send201(vertx, "CL-STATION-204", msg))
                .whenComplete((resp, err) -> ctx.verify(() -> {
                    assertThat(err).isNull();
                    assertThat(harness.chargingLimitSink.clearedEvents()).hasSize(1);
                    FakeChargingLimitSink.ClearedEvent c = harness.chargingLimitSink.clearedEvents().get(0);
                    assertThat(c.source()).isEqualTo(ChargingLimitSource.SO);
                    assertThat(c.evseId()).isEqualTo(3);
                    ctx.completeNow();
                }));
    }
}
