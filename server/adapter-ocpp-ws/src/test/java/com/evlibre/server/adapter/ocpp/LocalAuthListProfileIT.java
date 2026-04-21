package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.usecases.GetLocalListVersionUseCase;
import com.evlibre.server.core.usecases.SendLocalListUseCase;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
@Tag("integration")
class LocalAuthListProfileIT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("LAL-STATION");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void getLocalListVersion_returns_version(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "LAL-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("GetLocalListVersion", payload ->
                            Map.of("listVersion", 3));
                    var useCase = new GetLocalListVersionUseCase(harness.commandSender);
                    return useCase.getLocalListVersion(TENANT, STATION)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.status()).isEqualTo("3");
                                    assertThat(client.receivedCommands("GetLocalListVersion")).hasSize(1);
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
    void sendLocalList_full_accepted(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, "LAL-STATION", "ocpp1.6")
                .thenCompose(client -> {
                    client.onCommand("SendLocalList", "Accepted");
                    var useCase = new SendLocalListUseCase(harness.commandSender);
                    List<Map<String, Object>> authList = List.of(
                            Map.of("idTag", "TAG001", "idTagInfo", Map.of("status", "Accepted")));
                    return useCase.sendLocalList(TENANT, STATION, 1, "Full", authList)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    var cmd = client.receivedCommands("SendLocalList").get(0);
                                    assertThat(cmd.payload().get("listVersion").asInt()).isEqualTo(1);
                                    assertThat(cmd.payload().get("updateType").asText()).isEqualTo("Full");
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
