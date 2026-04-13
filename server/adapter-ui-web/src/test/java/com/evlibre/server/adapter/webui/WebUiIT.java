package com.evlibre.server.adapter.webui;

import com.evlibre.server.adapter.ocpp.OcppSessionManager;
import com.evlibre.server.test.fakes.FakeStationRepository;
import com.evlibre.server.test.fakes.FakeTenantRepository;
import com.evlibre.server.test.fakes.FakeTransactionRepository;
import com.evlibre.server.test.fixtures.Stations;
import com.evlibre.server.test.fixtures.Tenants;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
@Tag("integration")
class WebUiIT {

    private WebUiVerticle verticle;
    private WebClient webClient;
    private FakeTenantRepository tenantRepo;
    private FakeStationRepository stationRepo;
    private FakeTransactionRepository transactionRepo;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        tenantRepo = new FakeTenantRepository();
        stationRepo = new FakeStationRepository();
        transactionRepo = new FakeTransactionRepository();

        tenantRepo.save(Tenants.demo());

        verticle = new WebUiVerticle(tenantRepo, stationRepo, transactionRepo, new OcppSessionManager(), 0);
        webClient = WebClient.create(vertx);
        vertx.deployVerticle(verticle).onComplete(ctx.succeedingThenComplete());
    }

    @Test
    void root_returns_welcome_page(VertxTestContext ctx) {
        webClient.get(verticle.actualPort(), "localhost", "/")
            .send()
            .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.bodyAsString()).contains("evlibre");
                ctx.completeNow();
            })));
    }

    @Test
    void dashboard_returns_200_for_known_tenant(VertxTestContext ctx) {
        webClient.get(verticle.actualPort(), "localhost", "/demo-tenant/dashboard")
            .send()
            .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.bodyAsString()).contains("html");
                ctx.completeNow();
            })));
    }

    @Test
    void stations_returns_200_for_known_tenant(VertxTestContext ctx) {
        webClient.get(verticle.actualPort(), "localhost", "/demo-tenant/stations")
            .send()
            .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                assertThat(response.statusCode()).isEqualTo(200);
                ctx.completeNow();
            })));
    }

    @Test
    void stations_shows_registered_station(VertxTestContext ctx) {
        stationRepo.save(Stations.accepted(Tenants.DEMO_TENANT_ID));

        webClient.get(verticle.actualPort(), "localhost", "/demo-tenant/stations")
            .send()
            .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.bodyAsString()).contains("CHARGER-001");
                ctx.completeNow();
            })));
    }

    @Test
    void unknown_tenant_returns_403(VertxTestContext ctx) {
        webClient.get(verticle.actualPort(), "localhost", "/nonexistent/dashboard")
            .send()
            .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                assertThat(response.statusCode()).isEqualTo(403);
                ctx.completeNow();
            })));
    }

    @Test
    void unknown_route_returns_404(VertxTestContext ctx) {
        webClient.get(verticle.actualPort(), "localhost", "/notaroute")
            .send()
            .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
                assertThat(response.statusCode()).isEqualTo(404);
                ctx.completeNow();
            })));
    }
}
