package com.evlibre.server.adapter.webui;

import com.evlibre.server.adapter.ocpp.OcppSessionManager;
import com.evlibre.server.test.fakes.FakeStationCommandSenderV201;
import com.evlibre.server.test.fakes.FakeStationCommandSenderV201.SentCommand;
import com.evlibre.server.test.fakes.FakeStationRepository;
import com.evlibre.server.test.fakes.FakeTenantRepository;
import com.evlibre.server.test.fakes.FakeTransactionRepository;
import com.evlibre.server.test.fixtures.Tenants;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
@Tag("integration")
class V201CommandIT {

    private WebUiVerticle verticle;
    private WebClient webClient;
    private FakeStationCommandSenderV201 fakeV201;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        FakeTenantRepository tenantRepo = new FakeTenantRepository();
        FakeStationRepository stationRepo = new FakeStationRepository();
        FakeTransactionRepository transactionRepo = new FakeTransactionRepository();
        tenantRepo.save(Tenants.demo());

        fakeV201 = new FakeStationCommandSenderV201();

        verticle = new WebUiVerticle(
                tenantRepo, stationRepo, transactionRepo,
                new OcppSessionManager(), null, fakeV201, 0);
        webClient = WebClient.create(vertx);
        vertx.deployVerticle(verticle).onComplete(ctx.succeedingThenComplete());
    }

    @Test
    void reset_immediate_sends_type_wire_value(VertxTestContext ctx) {
        webClient.post(verticle.actualPort(), "localhost",
                        "/demo-tenant/stations/CHARGER-001/v201/reset?type=Immediate")
                .send()
                .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    SentCommand sent = fakeV201.lastCommand();
                    assertThat(sent).isNotNull();
                    assertThat(sent.action()).isEqualTo("Reset");
                    assertThat(sent.payload()).containsEntry("type", "Immediate");
                    assertThat(sent.payload()).doesNotContainKey("evseId");
                    ctx.completeNow();
                })));
    }

    @Test
    void reset_on_idle_with_evse_id(VertxTestContext ctx) {
        webClient.post(verticle.actualPort(), "localhost",
                        "/demo-tenant/stations/CHARGER-001/v201/reset?type=OnIdle&evseId=2")
                .send()
                .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    SentCommand sent = fakeV201.lastCommand();
                    assertThat(sent.action()).isEqualTo("Reset");
                    assertThat(sent.payload())
                            .containsEntry("type", "OnIdle")
                            .containsEntry("evseId", 2);
                    ctx.completeNow();
                })));
    }

    @Test
    void reset_rejects_unknown_type(VertxTestContext ctx) {
        webClient.post(verticle.actualPort(), "localhost",
                        "/demo-tenant/stations/CHARGER-001/v201/reset?type=Bogus")
                .send()
                .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.bodyAsString()).contains("error");
                    assertThat(fakeV201.commands()).isEmpty();
                    ctx.completeNow();
                })));
    }

    @Test
    void change_availability_station_wide_omits_evse(VertxTestContext ctx) {
        webClient.post(verticle.actualPort(), "localhost",
                        "/demo-tenant/stations/CHARGER-001/v201/change-availability?operationalStatus=Operative")
                .send()
                .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    SentCommand sent = fakeV201.lastCommand();
                    assertThat(sent.action()).isEqualTo("ChangeAvailability");
                    assertThat(sent.payload())
                            .containsEntry("operationalStatus", "Operative")
                            .doesNotContainKey("evse");
                    ctx.completeNow();
                })));
    }

    @Test
    void change_availability_evse_scope_wraps_id(VertxTestContext ctx) {
        MultiMap form = MultiMap.caseInsensitiveMultiMap()
                .add("operationalStatus", "Inoperative")
                .add("evseId", "3");
        webClient.post(verticle.actualPort(), "localhost",
                        "/demo-tenant/stations/CHARGER-001/v201/change-availability")
                .sendForm(form)
                .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                    SentCommand sent = fakeV201.lastCommand();
                    assertThat(sent.payload())
                            .containsEntry("operationalStatus", "Inoperative")
                            .containsKey("evse");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> evse = (Map<String, Object>) sent.payload().get("evse");
                    assertThat(evse)
                            .containsEntry("id", 3)
                            .doesNotContainKey("connectorId");
                    ctx.completeNow();
                })));
    }

    @Test
    void change_availability_connector_scope_adds_connectorId(VertxTestContext ctx) {
        MultiMap form = MultiMap.caseInsensitiveMultiMap()
                .add("operationalStatus", "Operative")
                .add("evseId", "2")
                .add("connectorId", "1");
        webClient.post(verticle.actualPort(), "localhost",
                        "/demo-tenant/stations/CHARGER-001/v201/change-availability")
                .sendForm(form)
                .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                    SentCommand sent = fakeV201.lastCommand();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> evse = (Map<String, Object>) sent.payload().get("evse");
                    assertThat(evse)
                            .containsEntry("id", 2)
                            .containsEntry("connectorId", 1);
                    ctx.completeNow();
                })));
    }

    @Test
    void request_start_wraps_idtoken_as_object(VertxTestContext ctx) {
        MultiMap form = MultiMap.caseInsensitiveMultiMap()
                .add("idToken", "TAG001")
                .add("tokenType", "ISO14443");
        webClient.post(verticle.actualPort(), "localhost",
                        "/demo-tenant/stations/CHARGER-001/v201/request-start")
                .sendForm(form)
                .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                    SentCommand sent = fakeV201.lastCommand();
                    assertThat(sent.action()).isEqualTo("RequestStartTransaction");
                    assertThat(sent.payload()).containsKey("remoteStartId");
                    assertThat(sent.payload().get("remoteStartId")).isInstanceOf(Integer.class);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> idToken = (Map<String, Object>) sent.payload().get("idToken");
                    assertThat(idToken)
                            .containsEntry("idToken", "TAG001")
                            .containsEntry("type", "ISO14443");
                    assertThat(sent.payload()).doesNotContainKey("evseId");
                    ctx.completeNow();
                })));
    }

    @Test
    void request_start_includes_evse_id_when_provided(VertxTestContext ctx) {
        MultiMap form = MultiMap.caseInsensitiveMultiMap()
                .add("idToken", "TAG002")
                .add("tokenType", "Central")
                .add("evseId", "3");
        webClient.post(verticle.actualPort(), "localhost",
                        "/demo-tenant/stations/CHARGER-001/v201/request-start")
                .sendForm(form)
                .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                    SentCommand sent = fakeV201.lastCommand();
                    assertThat(sent.payload()).containsEntry("evseId", 3);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> idToken = (Map<String, Object>) sent.payload().get("idToken");
                    assertThat(idToken).containsEntry("type", "Central");
                    ctx.completeNow();
                })));
    }

    @Test
    void request_start_rejects_missing_idtoken(VertxTestContext ctx) {
        MultiMap form = MultiMap.caseInsensitiveMultiMap()
                .add("tokenType", "Central");
        webClient.post(verticle.actualPort(), "localhost",
                        "/demo-tenant/stations/CHARGER-001/v201/request-start")
                .sendForm(form)
                .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                    assertThat(response.bodyAsString()).contains("error");
                    assertThat(fakeV201.commands()).isEmpty();
                    ctx.completeNow();
                })));
    }

    @Test
    void unlock_connector_requires_both_ids(VertxTestContext ctx) {
        MultiMap form = MultiMap.caseInsensitiveMultiMap()
                .add("evseId", "1");
        webClient.post(verticle.actualPort(), "localhost",
                        "/demo-tenant/stations/CHARGER-001/v201/unlock-connector")
                .sendForm(form)
                .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                    assertThat(response.bodyAsString()).contains("error");
                    assertThat(fakeV201.commands()).isEmpty();
                    ctx.completeNow();
                })));
    }

    @Test
    void unlock_connector_sends_evse_and_connector_ids(VertxTestContext ctx) {
        MultiMap form = MultiMap.caseInsensitiveMultiMap()
                .add("evseId", "2")
                .add("connectorId", "1");
        webClient.post(verticle.actualPort(), "localhost",
                        "/demo-tenant/stations/CHARGER-001/v201/unlock-connector")
                .sendForm(form)
                .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                    SentCommand sent = fakeV201.lastCommand();
                    assertThat(sent.action()).isEqualTo("UnlockConnector");
                    assertThat(sent.payload())
                            .containsEntry("evseId", 2)
                            .containsEntry("connectorId", 1);
                    ctx.completeNow();
                })));
    }

    @Test
    void clear_cache_sends_empty_payload(VertxTestContext ctx) {
        webClient.post(verticle.actualPort(), "localhost",
                        "/demo-tenant/stations/CHARGER-001/v201/clear-cache")
                .send()
                .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    SentCommand sent = fakeV201.lastCommand();
                    assertThat(sent.action()).isEqualTo("ClearCache");
                    assertThat(sent.payload()).isEmpty();
                    ctx.completeNow();
                })));
    }

}
