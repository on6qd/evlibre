package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.displaymessage.MessageInfo;
import com.evlibre.server.core.usecases.v201.GetDisplayMessagesUseCaseV201;
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
 * End-to-end display-message inventory flow: CSMS sends GetDisplayMessages,
 * station streams a multi-frame NotifyDisplayMessages, and the aggregated
 * MessageInfo list lands in the sink exactly once. Completes Block O
 * alongside {@link CustomerInformation201IT}.
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class DisplayMessages201IT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("DM-STATION-201");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void get_then_multi_frame_notify_concatenates_into_single_sink_event(
            Vertx vertx, VertxTestContext ctx) {
        final int requestId = 42;

        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetDisplayMessages", "Accepted");
                    GetDisplayMessagesUseCaseV201 useCase =
                            new GetDisplayMessagesUseCaseV201(harness.commandSender201);
                    return useCase.getDisplayMessages(TENANT, STATION, requestId, List.of(), null, null)
                            .thenCompose(result -> {
                                ctx.verify(() -> assertThat(result.isAccepted()).isTrue());
                                return client.send(notifyFrame("frame-0", requestId, true,
                                        messageInfo(1, "NormalCycle", "Welcome"),
                                        messageInfo(2, "InFront", "Charging now")));
                            })
                            .thenCompose(resp1 -> {
                                ctx.verify(() -> assertThat(harness.displayMessagesSink.events()).isEmpty());
                                return client.send(notifyFrame("frame-1", requestId, false,
                                        messageInfo(3, "AlwaysFront", "Emergency")));
                            })
                            .thenApply(respFinal -> {
                                ctx.verify(() -> {
                                    var events = harness.displayMessagesSink.events();
                                    assertThat(events).hasSize(1);
                                    assertThat(events.get(0).messages())
                                            .extracting(MessageInfo::id)
                                            .containsExactly(1, 2, 3);
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
    void single_frame_notify_fires_sink_with_just_that_frames_content(
            Vertx vertx, VertxTestContext ctx) {
        final int requestId = 7;

        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetDisplayMessages", "Accepted");
                    GetDisplayMessagesUseCaseV201 useCase =
                            new GetDisplayMessagesUseCaseV201(harness.commandSender201);
                    return useCase.getDisplayMessages(TENANT, STATION, requestId, List.of(), null, null)
                            .thenCompose(result -> {
                                ctx.verify(() -> assertThat(result.isAccepted()).isTrue());
                                return client.send(notifyFrame("single", requestId, false,
                                        messageInfo(5, "NormalCycle", "Single-shot")));
                            })
                            .thenApply(resp -> {
                                ctx.verify(() -> {
                                    var events = harness.displayMessagesSink.events();
                                    assertThat(events).hasSize(1);
                                    assertThat(events.get(0).messages())
                                            .extracting(MessageInfo::id)
                                            .containsExactly(5);
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

    private static String messageInfo(int id, String priority, String content) {
        return String.format(
                "{\"id\":%d,\"priority\":\"%s\","
                        + "\"message\":{\"format\":\"UTF8\",\"content\":\"%s\"}}",
                id, priority, content);
    }

    private static String notifyFrame(String msgId, int requestId, boolean tbc, String... messageInfos) {
        String arr = "[" + String.join(",", messageInfos) + "]";
        return String.format(
                "[2,\"%s\",\"NotifyDisplayMessages\","
                        + "{\"requestId\":%d,\"tbc\":%s,\"messageInfo\":%s}]",
                msgId, requestId, tbc, arr);
    }
}
