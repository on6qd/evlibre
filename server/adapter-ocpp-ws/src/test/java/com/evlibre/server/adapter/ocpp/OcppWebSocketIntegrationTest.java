package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.handler.v16.BootNotificationHandler16;
import com.evlibre.server.adapter.ocpp.handler.v201.BootNotificationHandler201;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.usecases.v16.RegisterStationUseCase;
import com.evlibre.server.test.fakes.*;
import com.evlibre.server.test.fixtures.Tenants;
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
        FakeTenantRepository tenantRepo = new FakeTenantRepository();
        tenantRepo.save(Tenants.demo());
        FakeStationRepository stationRepo = new FakeStationRepository();
        FakeOcppEventLog eventLog = new FakeOcppEventLog();
        FakeTimeProvider timeProvider = new FakeTimeProvider();

        RegisterStationUseCase registerUseCase = new RegisterStationUseCase(
                tenantRepo, stationRepo, eventLog, timeProvider, 900,
                (t, s) -> {});

        BootNotificationHandler16 bootHandler = new BootNotificationHandler16(registerUseCase, null, sessionManager, objectMapper);
        dispatcher.registerHandler(OcppProtocol.OCPP_16, "BootNotification", bootHandler);

        BootNotificationHandler201 bootHandler201 = new BootNotificationHandler201(registerUseCase, null, sessionManager, objectMapper);
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

    // OCPP 1.6 §4.2: any action other than BootNotification before the station has
    // been accepted must be rejected with SecurityError.
    @Test
    void non_boot_action_before_boot_returns_securityError(Vertx vertx, VertxTestContext ctx) {
        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setPort(verticle.actualPort())
                .setHost("localhost")
                .setURI("/ocpp/demo-tenant/CHARGER-001")
                .addSubProtocol("ocpp1.6");

        client.connect(options).onComplete(ctx.succeeding(ws -> {
            String req = "[2,\"msg-2\",\"Heartbeat\",{}]";
            ws.writeTextMessage(req);

            ws.textMessageHandler(msg -> ctx.verify(() -> {
                JsonNode response = objectMapper.readTree(msg);
                assertThat(response.get(0).asInt()).isEqualTo(4); // CALLERROR
                assertThat(response.get(2).asText()).isEqualTo("SecurityError");
                ws.close();
                ctx.completeNow();
            }));
        }));
    }

    @Test
    void missing_required_field_returns_protocolError(Vertx vertx, VertxTestContext ctx) {
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
                assertThat(response.get(2).asText()).isEqualTo("ProtocolError");
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
}
