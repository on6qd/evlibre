package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ReportBase;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedVariable;
import com.evlibre.server.core.usecases.v201.GetBaseReportUseCaseV201;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end device-model reporting: CSMS sends GetBaseReport, station streams
 * a multi-frame NotifyReport, and the aggregated reports land in the repo
 * exactly once together with a single completion event. Ties the outbound
 * (GetBaseReportUseCaseV201) and inbound (HandleNotifyReportUseCaseV201) legs
 * together — previously tested only in isolation.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class DeviceModelReporting201IT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("DM-STATION-201");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void get_base_report_then_multi_frame_notify_report_populates_repo_atomically(
            Vertx vertx, VertxTestContext ctx) {
        final int requestId = 42;

        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetBaseReport", "Accepted");
                    GetBaseReportUseCaseV201 useCase = new GetBaseReportUseCaseV201(harness.commandSender201);
                    return useCase.getBaseReport(TENANT, STATION, requestId, ReportBase.FULL_INVENTORY)
                            .thenCompose(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("GetBaseReport").get(0);
                                    assertThat(cmd.payload().get("requestId").asInt()).isEqualTo(requestId);
                                });

                                // Frame 1 (seqNo=0, tbc=true): two variables.
                                return client.send(notifyReportFrame(
                                        "frame-0", requestId, 0, true,
                                        entry("SecurityCtrlr", "BasicAuthPassword"),
                                        entry("SecurityCtrlr", "Identity")));
                            })
                            .thenCompose(resp1 -> {
                                // Buffered, not committed yet.
                                ctx.verify(() -> {
                                    assertThat(harness.deviceModelRepo.findAll(TENANT, STATION)).isEmpty();
                                    assertThat(harness.notifyReportCompletion.events()).isEmpty();
                                });

                                // Frame 2 (seqNo=1, tbc=true): two more variables.
                                return client.send(notifyReportFrame(
                                        "frame-1", requestId, 1, true,
                                        entry("OCPPCommCtrlr", "NetworkConfigurationPriority"),
                                        entry("OCPPCommCtrlr", "HeartbeatInterval")));
                            })
                            .thenCompose(resp2 -> {
                                ctx.verify(() -> {
                                    assertThat(harness.deviceModelRepo.findAll(TENANT, STATION)).isEmpty();
                                    assertThat(harness.notifyReportCompletion.events()).isEmpty();
                                });

                                // Final frame (seqNo=2, tbc=false): one last variable.
                                return client.send(notifyReportFrame(
                                        "frame-2", requestId, 2, false,
                                        entry("DeviceDataCtrlr", "ItemsPerMessageGetReport")));
                            })
                            .thenApply(respFinal -> {
                                ctx.verify(() -> {
                                    // All five reports committed in a single aggregated upsert.
                                    List<ReportedVariable> stored = harness.deviceModelRepo.findAll(TENANT, STATION);
                                    assertThat(stored).hasSize(5);
                                    assertThat(stored)
                                            .extracting(r -> r.component().name() + "/" + r.variable().name())
                                            .containsExactlyInAnyOrder(
                                                    "SecurityCtrlr/BasicAuthPassword",
                                                    "SecurityCtrlr/Identity",
                                                    "OCPPCommCtrlr/NetworkConfigurationPriority",
                                                    "OCPPCommCtrlr/HeartbeatInterval",
                                                    "DeviceDataCtrlr/ItemsPerMessageGetReport");

                                    // Completion fired exactly once with the matching requestId.
                                    assertThat(harness.notifyReportCompletion.events())
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
    void single_frame_notify_report_commits_and_signals_immediately(
            Vertx vertx, VertxTestContext ctx) {
        final int requestId = 7;

        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetBaseReport", "Accepted");
                    GetBaseReportUseCaseV201 useCase = new GetBaseReportUseCaseV201(harness.commandSender201);
                    return useCase.getBaseReport(TENANT, STATION, requestId, ReportBase.SUMMARY_INVENTORY)
                            .thenCompose(result -> {
                                ctx.verify(() -> assertThat(result.isAccepted()).isTrue());
                                return client.send(notifyReportFrame(
                                        "single", requestId, 0, false,
                                        entry("AuthCtrlr", "Enabled")));
                            })
                            .thenApply(resp -> {
                                ctx.verify(() -> {
                                    assertThat(harness.deviceModelRepo.findAll(TENANT, STATION))
                                            .extracting(r -> r.variable().name())
                                            .containsExactly("Enabled");
                                    assertThat(harness.notifyReportCompletion.events())
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

    private record Entry(String componentName, String variableName) {}

    private static Entry entry(String component, String variable) {
        return new Entry(component, variable);
    }

    private static String notifyReportFrame(String msgId, int requestId, int seqNo, boolean tbc, Entry... entries) {
        StringBuilder reportData = new StringBuilder("[");
        for (int i = 0; i < entries.length; i++) {
            if (i > 0) reportData.append(",");
            reportData.append(String.format(
                    "{\"component\":{\"name\":\"%s\"},"
                            + "\"variable\":{\"name\":\"%s\"},"
                            + "\"variableAttribute\":[{\"type\":\"Actual\",\"value\":\"v\"}]}",
                    entries[i].componentName(), entries[i].variableName()));
        }
        reportData.append("]");
        return String.format(
                "[2,\"%s\",\"NotifyReport\","
                        + "{\"requestId\":%d,\"generatedAt\":\"2026-04-23T10:00:00Z\","
                        + "\"seqNo\":%d,\"tbc\":%s,\"reportData\":%s}]",
                msgId, requestId, seqNo, tbc, reportData);
    }
}
