package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.AttributeType;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.ComponentCriterion;
import com.evlibre.server.core.domain.v201.devicemodel.ComponentVariableSelector;
import com.evlibre.server.core.domain.v201.devicemodel.GetVariableData;
import com.evlibre.server.core.domain.v201.devicemodel.GetVariableStatus;
import com.evlibre.server.core.domain.v201.devicemodel.ReportBase;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import com.evlibre.server.core.usecases.v201.GetBaseReportUseCaseV201;
import com.evlibre.server.core.usecases.v201.GetReportUseCaseV201;
import com.evlibre.server.core.usecases.v201.GetVariablesUseCaseV201;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CSMS-to-CS v2.0.1 Provisioning (Block B) commands over a
 * real WebSocket connection. Mirrors {@link CoreProfileCommandIT} but for the
 * v2.0.1 siblings — nothing here touches a v1.6 use case.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class ProvisioningCommandIT201 {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("CMD-STATION-201");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void get_base_report_full_inventory_accepted(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetBaseReport", "Accepted");
                    GetBaseReportUseCaseV201 useCase = new GetBaseReportUseCaseV201(harness.commandSender201);
                    return useCase.getBaseReport(TENANT, STATION, 42, ReportBase.FULL_INVENTORY)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(client.receivedCommands("GetBaseReport")).hasSize(1);
                                    var cmd = client.receivedCommands("GetBaseReport").get(0);
                                    assertThat(cmd.payload().get("requestId").asInt()).isEqualTo(42);
                                    assertThat(cmd.payload().get("reportBase").asText()).isEqualTo("FullInventory");
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
    void get_base_report_summary_inventory_rejected_propagates_status(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetBaseReport", "Rejected");
                    GetBaseReportUseCaseV201 useCase = new GetBaseReportUseCaseV201(harness.commandSender201);
                    return useCase.getBaseReport(TENANT, STATION, 1, ReportBase.SUMMARY_INVENTORY)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isFalse();
                                    assertThat(result.status()).isEqualTo("Rejected");
                                    var cmd = client.receivedCommands("GetBaseReport").get(0);
                                    assertThat(cmd.payload().get("reportBase").asText()).isEqualTo("SummaryInventory");
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
    void get_report_with_criteria_and_selector_accepted(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetReport", "Accepted");
                    GetReportUseCaseV201 useCase = new GetReportUseCaseV201(harness.commandSender201);
                    var selector = ComponentVariableSelector.of(
                            Component.of("SecurityCtrlr"), Variable.of("BasicAuthPassword"));
                    return useCase.getReport(TENANT, STATION, 99,
                                    Set.of(ComponentCriterion.PROBLEM, ComponentCriterion.ACTIVE),
                                    List.of(selector))
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("GetReport").get(0);
                                    assertThat(cmd.payload().get("requestId").asInt()).isEqualTo(99);
                                    var criteria = cmd.payload().get("componentCriteria");
                                    assertThat(criteria.isArray()).isTrue();
                                    assertThat(criteria.size()).isEqualTo(2);
                                    var componentVariable = cmd.payload().get("componentVariable");
                                    assertThat(componentVariable.size()).isEqualTo(1);
                                    var entry = componentVariable.get(0);
                                    assertThat(entry.get("component").get("name").asText())
                                            .isEqualTo("SecurityCtrlr");
                                    assertThat(entry.get("variable").get("name").asText())
                                            .isEqualTo("BasicAuthPassword");
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
    void get_report_empty_result_set_propagated(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetReport", "EmptyResultSet");
                    GetReportUseCaseV201 useCase = new GetReportUseCaseV201(harness.commandSender201);
                    return useCase.getReport(TENANT, STATION, 1,
                                    Set.of(ComponentCriterion.ENABLED), List.of())
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.status()).isEqualTo("EmptyResultSet");
                                    assertThat(result.isAccepted()).isFalse();
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
    void get_report_on_v16_session_rejected_by_protocol_guard(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp1.6")
                .thenCompose(client -> {
                    GetReportUseCaseV201 useCase = new GetReportUseCaseV201(harness.commandSender201);
                    return useCase.getReport(TENANT, STATION, 1,
                                    Set.of(ComponentCriterion.ACTIVE), List.of())
                            .handle((result, err) -> {
                                ctx.verify(() -> {
                                    assertThat(err).isNotNull();
                                    assertThat(err.getCause())
                                            .isInstanceOf(IllegalStateException.class)
                                            .hasMessageContaining("Cannot send GetReport via OCPP_201");
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
    void get_variables_inline_results_decoded(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    // Station responds with two result entries: one Accepted with a value,
                    // one Rejected (WriteOnly variable) with statusInfo but no attributeValue.
                    client.onCommand("GetVariables", payload -> Map.of("getVariableResult", List.of(
                            Map.of(
                                    "attributeStatus", "Accepted",
                                    "attributeValue", "changeme",
                                    "component", Map.of("name", "SecurityCtrlr"),
                                    "variable", Map.of("name", "BasicAuthPassword")),
                            Map.of(
                                    "attributeStatus", "Rejected",
                                    "attributeStatusInfo", Map.of("reasonCode", "WriteOnly"),
                                    "component", Map.of("name", "SecurityCtrlr"),
                                    "variable", Map.of("name", "Identity")))));
                    GetVariablesUseCaseV201 useCase = new GetVariablesUseCaseV201(harness.commandSender201);
                    return useCase.getVariables(TENANT, STATION, List.of(
                                    GetVariableData.of(Component.of("SecurityCtrlr"), Variable.of("BasicAuthPassword")),
                                    GetVariableData.of(Component.of("SecurityCtrlr"), Variable.of("Identity"),
                                            AttributeType.ACTUAL)))
                            .thenApply(results -> {
                                ctx.verify(() -> {
                                    var cmd = client.receivedCommands("GetVariables").get(0);
                                    var data = cmd.payload().get("getVariableData");
                                    assertThat(data.isArray()).isTrue();
                                    assertThat(data.size()).isEqualTo(2);
                                    // First entry has no attributeType on the wire (domain null).
                                    assertThat(data.get(0).has("attributeType")).isFalse();
                                    // Second entry serialised ACTUAL explicitly.
                                    assertThat(data.get(1).get("attributeType").asText()).isEqualTo("Actual");

                                    assertThat(results).hasSize(2);
                                    assertThat(results.get(0).status()).isEqualTo(GetVariableStatus.ACCEPTED);
                                    assertThat(results.get(0).attributeValue()).isEqualTo("changeme");
                                    assertThat(results.get(1).status()).isEqualTo(GetVariableStatus.REJECTED);
                                    assertThat(results.get(1).attributeValue()).isNull();
                                    assertThat(results.get(1).statusInfoReason()).isEqualTo("WriteOnly");
                                });
                                client.close();
                                return results;
                            });
                })
                .whenComplete((r, err) -> {
                    if (err != null) ctx.failNow(err);
                    else ctx.completeNow();
                });
    }

    @Test
    void get_variables_on_v16_session_rejected_by_protocol_guard(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp1.6")
                .thenCompose(client -> {
                    GetVariablesUseCaseV201 useCase = new GetVariablesUseCaseV201(harness.commandSender201);
                    return useCase.getVariables(TENANT, STATION, List.of(
                                    GetVariableData.of(Component.of("C"), Variable.of("V"))))
                            .handle((result, err) -> {
                                ctx.verify(() -> {
                                    assertThat(err).isNotNull();
                                    assertThat(err.getCause())
                                            .isInstanceOf(IllegalStateException.class)
                                            .hasMessageContaining("Cannot send GetVariables via OCPP_201");
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
    void get_base_report_on_v16_session_rejected_by_protocol_guard(Vertx vertx, VertxTestContext ctx) {
        // Cross-protocol: a v1.6-negotiated session must not accept a v2.0.1 outbound
        // command. The v201 port in OcppStationCommandSender fails fast.
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp1.6")
                .thenCompose(client -> {
                    GetBaseReportUseCaseV201 useCase = new GetBaseReportUseCaseV201(harness.commandSender201);
                    return useCase.getBaseReport(TENANT, STATION, 1, ReportBase.FULL_INVENTORY)
                            .handle((result, err) -> {
                                ctx.verify(() -> {
                                    assertThat(err).isNotNull();
                                    assertThat(err.getCause())
                                            .isInstanceOf(IllegalStateException.class)
                                            .hasMessageContaining("Cannot send GetBaseReport via OCPP_201");
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
