package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.CustomerInformationTarget;
import com.evlibre.server.core.usecases.v201.CustomerInformationUseCaseV201;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end customer-information flow: CSMS sends CustomerInformation with
 * report=true, station streams a multi-frame NotifyCustomerInformation, and
 * the per-frame data chunks land in the sink as a single concatenated
 * string exactly once. Pairs with {@link MonitoringReporting201IT} for
 * Block O.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class CustomerInformation201IT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("CI-STATION-201");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void customer_information_then_multi_frame_notify_concatenates_into_single_sink_event(
            Vertx vertx, VertxTestContext ctx) {
        final int requestId = 42;

        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("CustomerInformation", "Accepted");
                    CustomerInformationUseCaseV201 useCase =
                            new CustomerInformationUseCaseV201(harness.commandSender201);
                    return useCase.customerInformation(TENANT, STATION, requestId, true, false,
                                    CustomerInformationTarget.byIdentifier("CUST-001"))
                            .thenCompose(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("CustomerInformation").get(0);
                                    assertThat(cmd.payload().get("customerIdentifier").asText())
                                            .isEqualTo("CUST-001");
                                });
                                return client.send(notifyFrame("frame-0", requestId, 0, true,
                                        "Name: John Doe. "));
                            })
                            .thenCompose(resp1 -> {
                                ctx.verify(() -> assertThat(harness.customerInformationSink.events()).isEmpty());
                                return client.send(notifyFrame("frame-1", requestId, 1, true,
                                        "Last transaction: 2026-04-20. "));
                            })
                            .thenCompose(resp2 -> {
                                ctx.verify(() -> assertThat(harness.customerInformationSink.events()).isEmpty());
                                return client.send(notifyFrame("frame-2", requestId, 2, false,
                                        "Balance: $0.00."));
                            })
                            .thenApply(respFinal -> {
                                ctx.verify(() -> {
                                    var events = harness.customerInformationSink.events();
                                    assertThat(events).hasSize(1);
                                    assertThat(events.get(0).data()).isEqualTo(
                                            "Name: John Doe. Last transaction: 2026-04-20. Balance: $0.00.");
                                    assertThat(events.get(0).requestId()).isEqualTo(requestId);
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
    void single_frame_notify_fires_sink_with_only_that_frames_data(
            Vertx vertx, VertxTestContext ctx) {
        final int requestId = 7;

        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("CustomerInformation", "Accepted");
                    CustomerInformationUseCaseV201 useCase =
                            new CustomerInformationUseCaseV201(harness.commandSender201);
                    return useCase.customerInformation(TENANT, STATION, requestId, true, false,
                                    CustomerInformationTarget.none())
                            .thenCompose(result -> {
                                ctx.verify(() -> assertThat(result.isAccepted()).isTrue());
                                return client.send(notifyFrame("single", requestId, 0, false,
                                        "single-shot reply"));
                            })
                            .thenApply(resp -> {
                                ctx.verify(() -> {
                                    var events = harness.customerInformationSink.events();
                                    assertThat(events).extracting(e -> e.data())
                                            .containsExactly("single-shot reply");
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

    private static String notifyFrame(String msgId, int requestId, int seqNo, boolean tbc, String data) {
        String escapedData = data.replace("\"", "\\\"");
        return String.format(
                "[2,\"%s\",\"NotifyCustomerInformation\","
                        + "{\"requestId\":%d,\"generatedAt\":\"2026-04-23T10:00:00Z\","
                        + "\"seqNo\":%d,\"tbc\":%s,\"data\":\"%s\"}]",
                msgId, requestId, seqNo, tbc, escapedData);
    }
}
