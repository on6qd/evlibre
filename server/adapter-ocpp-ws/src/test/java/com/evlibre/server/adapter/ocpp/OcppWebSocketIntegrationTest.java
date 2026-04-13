package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.handler.v16.BootNotificationHandler16;
import com.evlibre.server.adapter.ocpp.handler.v201.BootNotificationHandler201;
import com.evlibre.server.core.domain.model.Tenant;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.OcppEventLogPort;
import com.evlibre.server.core.domain.ports.outbound.StationRepositoryPort;
import com.evlibre.server.core.domain.ports.outbound.TenantRepositoryPort;
import com.evlibre.server.core.usecases.RegisterStationUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
class OcppWebSocketIntegrationTest {

    private OcppWebSocketVerticle verticle;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        objectMapper = new ObjectMapper();
        OcppMessageCodec codec = new OcppMessageCodec(objectMapper);
        OcppSchemaValidator schemaValidator = new OcppSchemaValidator();
        OcppMessageDispatcher dispatcher = new OcppMessageDispatcher();
        OcppSessionManager sessionManager = new OcppSessionManager();
        OcppProtocolNegotiator negotiator = new OcppProtocolNegotiator();

        // Set up fake repos
        FakeTenantRepo tenantRepo = new FakeTenantRepo();
        tenantRepo.save(Tenant.builder()
                .id(UUID.randomUUID())
                .tenantId(new TenantId("demo-tenant"))
                .companyName("Demo")
                .createdAt(Instant.now())
                .build());

        FakeStationRepo stationRepo = new FakeStationRepo();
        FakeEventLog eventLog = new FakeEventLog();

        RegisterStationUseCase registerUseCase = new RegisterStationUseCase(
                tenantRepo, stationRepo, eventLog, Instant::now, 900,
                (t, s) -> {});

        BootNotificationHandler16 bootHandler = new BootNotificationHandler16(registerUseCase, null, objectMapper);
        dispatcher.registerHandler(OcppProtocol.OCPP_16, "BootNotification", bootHandler);

        BootNotificationHandler201 bootHandler201 = new BootNotificationHandler201(registerUseCase, null, objectMapper);
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "BootNotification", bootHandler201);

        OcppPendingCallManager pendingCallManager = new OcppPendingCallManager();
        verticle = new OcppWebSocketVerticle(0, 60, codec, schemaValidator, dispatcher, sessionManager, negotiator, pendingCallManager, (t, s) -> {});
        vertx.deployVerticle(verticle).onComplete(ctx.succeedingThenComplete());
    }

    @Test
    void bootNotification_returns_accepted(Vertx vertx, VertxTestContext ctx) {
        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setPort(verticle.actualPort())
                .setHost("localhost")
                .setURI("/ocpp/demo-tenant/CHARGER-001")
                .addSubProtocol("ocpp1.6");

        client.connect(options).onComplete(ctx.succeeding(ws -> {
            String bootReq = "[2,\"msg-1\",\"BootNotification\","
                    + "{\"chargePointVendor\":\"ABB\",\"chargePointModel\":\"Terra AC\"}]";
            ws.writeTextMessage(bootReq);

            ws.textMessageHandler(msg -> ctx.verify(() -> {
                JsonNode response = objectMapper.readTree(msg);
                assertThat(response.isArray()).isTrue();
                assertThat(response.get(0).asInt()).isEqualTo(3); // CALLRESULT
                assertThat(response.get(1).asText()).isEqualTo("msg-1");
                assertThat(response.get(2).get("status").asText()).isEqualTo("Accepted");
                assertThat(response.get(2).has("currentTime")).isTrue();
                assertThat(response.get(2).get("interval").asInt()).isEqualTo(900);
                ws.close();
                ctx.completeNow();
            }));
        }));
    }

    @Test
    void unknown_action_returns_notImplemented(Vertx vertx, VertxTestContext ctx) {
        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setPort(verticle.actualPort())
                .setHost("localhost")
                .setURI("/ocpp/demo-tenant/CHARGER-001")
                .addSubProtocol("ocpp1.6");

        client.connect(options).onComplete(ctx.succeeding(ws -> {
            String req = "[2,\"msg-2\",\"DataTransfer\",{\"vendorId\":\"test\"}]";
            ws.writeTextMessage(req);

            ws.textMessageHandler(msg -> ctx.verify(() -> {
                JsonNode response = objectMapper.readTree(msg);
                assertThat(response.get(0).asInt()).isEqualTo(4); // CALLERROR
                assertThat(response.get(2).asText()).isEqualTo("NotImplemented");
                ws.close();
                ctx.completeNow();
            }));
        }));
    }

    @Test
    void invalid_payload_returns_formationViolation(Vertx vertx, VertxTestContext ctx) {
        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setPort(verticle.actualPort())
                .setHost("localhost")
                .setURI("/ocpp/demo-tenant/CHARGER-001")
                .addSubProtocol("ocpp1.6");

        client.connect(options).onComplete(ctx.succeeding(ws -> {
            // Missing required chargePointModel
            String req = "[2,\"msg-3\",\"BootNotification\",{\"chargePointVendor\":\"ABB\"}]";
            ws.writeTextMessage(req);

            ws.textMessageHandler(msg -> ctx.verify(() -> {
                JsonNode response = objectMapper.readTree(msg);
                assertThat(response.get(0).asInt()).isEqualTo(4); // CALLERROR
                assertThat(response.get(2).asText()).isEqualTo("FormationViolation");
                ws.close();
                ctx.completeNow();
            }));
        }));
    }

    @Test
    void ocpp201_bootNotification_returns_accepted(Vertx vertx, VertxTestContext ctx) {
        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setPort(verticle.actualPort())
                .setHost("localhost")
                .setURI("/ocpp/demo-tenant/CHARGER-201")
                .addSubProtocol("ocpp2.0.1");

        client.connect(options).onComplete(ctx.succeeding(ws -> {
            String bootReq = "[2,\"msg-201\",\"BootNotification\","
                    + "{\"chargingStation\":{\"vendorName\":\"ABB\",\"model\":\"Terra AC\"},\"reason\":\"PowerUp\"}]";
            ws.writeTextMessage(bootReq);

            ws.textMessageHandler(msg -> ctx.verify(() -> {
                JsonNode response = objectMapper.readTree(msg);
                assertThat(response.isArray()).isTrue();
                assertThat(response.get(0).asInt()).isEqualTo(3);
                assertThat(response.get(1).asText()).isEqualTo("msg-201");
                assertThat(response.get(2).get("status").asText()).isEqualTo("Accepted");
                assertThat(response.get(2).has("currentTime")).isTrue();
                assertThat(response.get(2).get("interval").asInt()).isEqualTo(900);
                ws.close();
                ctx.completeNow();
            }));
        }));
    }

    // Minimal fakes for the integration test

    static class FakeTenantRepo implements TenantRepositoryPort {
        private final Map<String, Tenant> store = new ConcurrentHashMap<>();
        @Override public void save(Tenant tenant) { store.put(tenant.tenantId().value(), tenant); }
        @Override public Optional<Tenant> findByTenantId(TenantId id) { return Optional.ofNullable(store.get(id.value())); }
    }

    static class FakeStationRepo implements StationRepositoryPort {
        private final Map<UUID, com.evlibre.server.core.domain.model.ChargingStation> store = new ConcurrentHashMap<>();
        @Override public void save(com.evlibre.server.core.domain.model.ChargingStation s) { store.put(s.id(), s); }
        @Override public Optional<com.evlibre.server.core.domain.model.ChargingStation> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<com.evlibre.server.core.domain.model.ChargingStation> findByTenantAndIdentity(TenantId t, com.evlibre.common.model.ChargePointIdentity i) {
            return store.values().stream().filter(s -> s.tenantId().equals(t) && s.identity().equals(i)).findFirst();
        }
        @Override public java.util.List<com.evlibre.server.core.domain.model.ChargingStation> findByTenant(TenantId t) {
            return store.values().stream().filter(s -> s.tenantId().equals(t)).toList();
        }
    }

    static class FakeEventLog implements OcppEventLogPort {
        @Override public void logEvent(String s, String m, String a, String d, String p) {}
    }
}
