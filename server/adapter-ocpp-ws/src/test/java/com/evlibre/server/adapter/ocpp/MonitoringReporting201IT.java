package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedMonitoring;
import com.evlibre.server.core.usecases.v201.GetMonitoringReportUseCaseV201;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end monitor reporting: CSMS sends GetMonitoringReport, station
 * streams a multi-frame NotifyMonitoringReport, and the aggregated reports
 * land in the monitor repo exactly once together with a single completion
 * event. Ties the outbound (GetMonitoringReportUseCaseV201) and inbound
 * (HandleNotifyMonitoringReportUseCaseV201) legs together — mirrors
 * {@link DeviceModelReporting201IT} for the monitoring block.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class MonitoringReporting201IT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("MON-STATION-201");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void get_monitoring_report_then_multi_frame_notify_populates_repo_atomically(
            Vertx vertx, VertxTestContext ctx) {
        final int requestId = 42;

        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetMonitoringReport", "Accepted");
                    GetMonitoringReportUseCaseV201 useCase =
                            new GetMonitoringReportUseCaseV201(harness.commandSender201);
                    return useCase.getMonitoringReport(TENANT, STATION, requestId, Set.of(), List.of())
                            .thenCompose(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("GetMonitoringReport").get(0);
                                    assertThat(cmd.payload().get("requestId").asInt()).isEqualTo(requestId);
                                });

                                return client.send(notifyMonitoringFrame(
                                        "frame-0", requestId, 0, true,
                                        entry("EVSE", "Power", monitor(1, "UpperThreshold", 7400.0, 4)),
                                        entry("EVSE", "Temperature", monitor(2, "UpperThreshold", 80.0, 3))));
                            })
                            .thenCompose(resp1 -> {
                                ctx.verify(() -> {
                                    assertThat(harness.monitorRepo.findAll(TENANT, STATION)).isEmpty();
                                    assertThat(harness.notifyMonitoringReportCompletion.events()).isEmpty();
                                });

                                return client.send(notifyMonitoringFrame(
                                        "frame-1", requestId, 1, true,
                                        entry("OCPPCommCtrlr", "HeartbeatInterval",
                                                monitor(3, "Periodic", 300.0, 5))));
                            })
                            .thenCompose(resp2 -> {
                                ctx.verify(() -> {
                                    assertThat(harness.monitorRepo.findAll(TENANT, STATION)).isEmpty();
                                    assertThat(harness.notifyMonitoringReportCompletion.events()).isEmpty();
                                });

                                return client.send(notifyMonitoringFrame(
                                        "frame-2", requestId, 2, false,
                                        entry("ChargingStation", "Available",
                                                monitor(4, "Delta", 1.0, 6))));
                            })
                            .thenApply(respFinal -> {
                                ctx.verify(() -> {
                                    List<ReportedMonitoring> stored = harness.monitorRepo.findAll(TENANT, STATION);
                                    assertThat(stored).hasSize(4);
                                    assertThat(stored)
                                            .extracting(r -> r.component().name() + "/" + r.variable().name())
                                            .containsExactlyInAnyOrder(
                                                    "EVSE/Power",
                                                    "EVSE/Temperature",
                                                    "OCPPCommCtrlr/HeartbeatInterval",
                                                    "ChargingStation/Available");

                                    assertThat(harness.notifyMonitoringReportCompletion.events())
                                            .extracting(e -> e.requestId())
                                            .containsExactly(requestId);
                                });
                                client.close();
                                return respFinal;
                            });
                })
                .whenComplete((r, err) -> {
                    if (err != null) ctx.failNow(err);
                    else ctx.completeNow();
                });
    }

    @Test
    void single_frame_notify_monitoring_report_commits_and_signals_immediately(
            Vertx vertx, VertxTestContext ctx) {
        final int requestId = 7;

        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetMonitoringReport", "Accepted");
                    GetMonitoringReportUseCaseV201 useCase =
                            new GetMonitoringReportUseCaseV201(harness.commandSender201);
                    return useCase.getMonitoringReport(TENANT, STATION, requestId, Set.of(), List.of())
                            .thenCompose(result -> {
                                ctx.verify(() -> assertThat(result.isAccepted()).isTrue());
                                return client.send(notifyMonitoringFrame(
                                        "single", requestId, 0, false,
                                        entry("AuthCtrlr", "Enabled",
                                                monitor(1, "UpperThreshold", 1.0, 5))));
                            })
                            .thenApply(resp -> {
                                ctx.verify(() -> {
                                    assertThat(harness.monitorRepo.findAll(TENANT, STATION))
                                            .extracting(r -> r.variable().name())
                                            .containsExactly("Enabled");
                                    assertThat(harness.notifyMonitoringReportCompletion.events())
                                            .extracting(e -> e.requestId())
                                            .containsExactly(requestId);
                                });
                                client.close();
                                return resp;
                            });
                })
                .whenComplete((r, err) -> {
                    if (err != null) ctx.failNow(err);
                    else ctx.completeNow();
                });
    }

    private record Entry(String componentName, String variableName, String monitorsJson) {}

    private static Entry entry(String component, String variable, String... monitorsJson) {
        return new Entry(component, variable, "[" + String.join(",", monitorsJson) + "]");
    }

    private static String monitor(int id, String type, double value, int severity) {
        return String.format("{\"id\":%d,\"transaction\":false,\"value\":%s,\"type\":\"%s\",\"severity\":%d}",
                id, value, type, severity);
    }

    private static String notifyMonitoringFrame(String msgId, int requestId, int seqNo, boolean tbc, Entry... entries) {
        StringBuilder monitorArr = new StringBuilder("[");
        for (int i = 0; i < entries.length; i++) {
            if (i > 0) monitorArr.append(",");
            monitorArr.append(String.format(
                    "{\"component\":{\"name\":\"%s\"},"
                            + "\"variable\":{\"name\":\"%s\"},"
                            + "\"variableMonitoring\":%s}",
                    entries[i].componentName(), entries[i].variableName(), entries[i].monitorsJson()));
        }
        monitorArr.append("]");
        return String.format(
                "[2,\"%s\",\"NotifyMonitoringReport\","
                        + "{\"requestId\":%d,\"generatedAt\":\"2026-04-23T10:00:00Z\","
                        + "\"seqNo\":%d,\"tbc\":%s,\"monitor\":%s}]",
                msgId, requestId, seqNo, tbc, monitorArr);
    }
}
