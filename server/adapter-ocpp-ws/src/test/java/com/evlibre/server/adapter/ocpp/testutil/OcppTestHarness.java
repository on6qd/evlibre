package com.evlibre.server.adapter.ocpp.testutil;

import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.*;
import com.evlibre.server.adapter.ocpp.handler.v16.*;
import com.evlibre.server.adapter.ocpp.handler.v201.*;
import com.evlibre.server.core.domain.model.AuthorizationStatus;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.usecases.*;
import com.evlibre.server.test.fakes.*;
import com.evlibre.server.test.fixtures.Tenants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.junit5.VertxTestContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Reusable test harness that wires up a full OCPP server with all handlers
 * registered and fake persistence. Used by integration tests to avoid
 * duplicating 50+ lines of setup code.
 */
public class OcppTestHarness {

    public final FakeTenantRepository tenantRepo = new FakeTenantRepository();
    public final FakeStationRepository stationRepo = new FakeStationRepository();
    public final FakeTransactionRepository transactionRepo = new FakeTransactionRepository();
    public final FakeAuthorizationRepository authRepo = new FakeAuthorizationRepository();
    public final FakeOcppEventLog eventLog = new FakeOcppEventLog();
    public final FakeTimeProvider timeProvider = new FakeTimeProvider();

    public final ObjectMapper objectMapper;
    public final OcppWebSocketVerticle verticle;
    public final OcppSessionManager sessionManager;
    public final OcppStationCommandSender commandSender;

    public OcppTestHarness() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Seed demo data
        tenantRepo.save(Tenants.demo());
        authRepo.addAuthorization(Tenants.DEMO_TENANT_ID, "TAG001", AuthorizationStatus.ACCEPTED);
        authRepo.addAuthorization(Tenants.DEMO_TENANT_ID, "TAG002", AuthorizationStatus.ACCEPTED);

        // Wire use cases
        RegisterStationUseCase registerStation = new RegisterStationUseCase(
                tenantRepo, stationRepo, eventLog, timeProvider, 900,
                (t, s) -> {});
        HandleHeartbeatUseCase handleHeartbeat = new HandleHeartbeatUseCase(stationRepo, timeProvider,
                (t, s) -> {});
        HandleStatusNotificationUseCase handleStatusNotification = new HandleStatusNotificationUseCase(eventLog);
        AuthorizeUseCase authorize = new AuthorizeUseCase(authRepo);
        StartTransactionUseCase startTransaction = new StartTransactionUseCase(authorize, transactionRepo, stationRepo,
                new FakeReservationRepository());
        StopTransactionUseCase stopTransaction = new StopTransactionUseCase(transactionRepo);
        HandleMeterValuesUseCase handleMeterValues = new HandleMeterValuesUseCase(eventLog);
        HandleTransactionEventUseCase handleTransactionEvent = new HandleTransactionEventUseCase(eventLog);
        HandleDataTransferUseCase handleDataTransfer = new HandleDataTransferUseCase(eventLog);
        HandleDiagnosticsStatusUseCase handleDiagnosticsStatus = new HandleDiagnosticsStatusUseCase(eventLog);
        HandleFirmwareStatusUseCase handleFirmwareStatus = new HandleFirmwareStatusUseCase(eventLog);

        // OCPP infrastructure
        OcppMessageCodec codec = new OcppMessageCodec(objectMapper);
        OcppSchemaValidator schemaValidator = new OcppSchemaValidator();
        OcppMessageDispatcher dispatcher = new OcppMessageDispatcher();
        sessionManager = new OcppSessionManager();
        OcppProtocolNegotiator negotiator = new OcppProtocolNegotiator();
        OcppPendingCallManager pendingCallManager = new OcppPendingCallManager();
        commandSender = new OcppStationCommandSender(sessionManager, codec, pendingCallManager, objectMapper);

        // Register OCPP 1.6 handlers
        dispatcher.registerHandler(OcppProtocol.OCPP_16, "BootNotification",
                new BootNotificationHandler16(registerStation, null, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_16, "Heartbeat",
                new HeartbeatHandler16(handleHeartbeat, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_16, "StatusNotification",
                new StatusNotificationHandler16(handleStatusNotification, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_16, "Authorize",
                new AuthorizeHandler16(authorize, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_16, "StartTransaction",
                new StartTransactionHandler16(startTransaction, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_16, "StopTransaction",
                new StopTransactionHandler16(stopTransaction, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_16, "MeterValues",
                new MeterValuesHandler16(handleMeterValues, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_16, "DataTransfer",
                new DataTransferHandler16(handleDataTransfer, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_16, "DiagnosticsStatusNotification",
                new DiagnosticsStatusNotificationHandler16(handleDiagnosticsStatus, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_16, "FirmwareStatusNotification",
                new FirmwareStatusNotificationHandler16(handleFirmwareStatus, objectMapper));

        // Register OCPP 2.0.1 handlers
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "BootNotification",
                new BootNotificationHandler201(registerStation, null, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "Heartbeat",
                new HeartbeatHandler201(handleHeartbeat, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "StatusNotification",
                new StatusNotificationHandler201(handleStatusNotification, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "Authorize",
                new AuthorizeHandler201(authorize, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "TransactionEvent",
                new TransactionEventHandler201(handleTransactionEvent, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "MeterValues",
                new MeterValuesHandler201(handleMeterValues, objectMapper));

        verticle = new OcppWebSocketVerticle(0, 60, codec, schemaValidator, dispatcher, sessionManager, negotiator, pendingCallManager, (t, s) -> {});
    }

    /**
     * Deploy the verticle. Call this in @BeforeEach.
     */
    public void deploy(Vertx vertx, VertxTestContext ctx) {
        vertx.deployVerticle(verticle).onComplete(ctx.succeedingThenComplete());
    }

    public int port() {
        return verticle.actualPort();
    }

    /**
     * Connect a WebSocket client and send a message, returning the parsed response.
     */
    public CompletableFuture<JsonNode> sendAndReceive(Vertx vertx, String tenantId, String stationId,
                                                       String subProtocol, String message) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setPort(port())
                .setHost("localhost")
                .setURI("/ocpp/" + tenantId + "/" + stationId)
                .addSubProtocol(subProtocol);

        client.connect(options).onComplete(ar -> {
            if (ar.failed()) {
                future.completeExceptionally(ar.cause());
                return;
            }
            var ws = ar.result();
            ws.textMessageHandler(msg -> {
                try {
                    JsonNode parsed = objectMapper.readTree(msg);
                    // Close and wait for server to unregister before completing
                    ws.close().onComplete(closeAr -> future.complete(parsed));
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            ws.writeTextMessage(message);
        });

        return future.orTimeout(5, TimeUnit.SECONDS);
    }

    /**
     * Convenience: send to demo-tenant with OCPP 1.6.
     */
    public CompletableFuture<JsonNode> send16(Vertx vertx, String stationId, String message) {
        return sendAndReceive(vertx, "demo-tenant", stationId, "ocpp1.6", message);
    }

    /**
     * Convenience: send to demo-tenant with OCPP 2.0.1.
     */
    public CompletableFuture<JsonNode> send201(Vertx vertx, String stationId, String message) {
        return sendAndReceive(vertx, "demo-tenant", stationId, "ocpp2.0.1", message);
    }
}
