package com.evlibre.server.adapter.ocpp.testutil;

import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.*;
import com.evlibre.server.adapter.ocpp.handler.v16.*;
import com.evlibre.server.adapter.ocpp.handler.v201.*;
import com.evlibre.server.core.domain.v16.model.AuthorizationStatus;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.usecases.v16.*;
import com.evlibre.server.core.usecases.v201.AuthorizeUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleDataTransferUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleHeartbeatUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleMeterValuesUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleClearedChargingLimitUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleFirmwareStatusNotificationUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleGet15118EVCertificateUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleGetCertificateStatusUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleLogStatusNotificationUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleNotifyChargingLimitUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleNotifyEventUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleNotifyCustomerInformationUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleNotifyDisplayMessagesUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleNotifyMonitoringReportUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandlePublishFirmwareStatusNotificationUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleNotifyEVChargingNeedsUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleNotifyEVChargingScheduleUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleNotifyReportUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleReportChargingProfilesUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleSecurityEventNotificationUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleSignCertificateUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleStatusNotificationUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleTransactionEventUseCase;
import com.evlibre.server.core.usecases.v201.RegisterStationUseCaseV201;
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
    public final FakeDeviceModelRepository deviceModelRepo = new FakeDeviceModelRepository();
    public final FakeNotifyReportCompletionPublisher notifyReportCompletion = new FakeNotifyReportCompletionPublisher();
    public final FakeMonitorRepository monitorRepo = new FakeMonitorRepository();
    public final FakeNotifyMonitoringReportCompletionPublisher notifyMonitoringReportCompletion = new FakeNotifyMonitoringReportCompletionPublisher();
    public final FakeCustomerInformationSink customerInformationSink = new FakeCustomerInformationSink();
    public final FakeDisplayMessagesSink displayMessagesSink = new FakeDisplayMessagesSink();
    public final FakeReportChargingProfilesSink reportChargingProfilesSink = new FakeReportChargingProfilesSink();
    public final FakeChargingLimitSink chargingLimitSink = new FakeChargingLimitSink();
    public final FakeEVChargingSink evChargingSink = new FakeEVChargingSink();
    public final FakeFirmwareStatusSink firmwareStatusSink = new FakeFirmwareStatusSink();
    public final FakeLogStatusSink logStatusSink = new FakeLogStatusSink();
    public final FakeNotifyEventSink notifyEventSink = new FakeNotifyEventSink();
    public final FakePublishFirmwareStatusSink publishFirmwareStatusSink = new FakePublishFirmwareStatusSink();
    public final FakeSecurityEventSink securityEventSink = new FakeSecurityEventSink();
    public final FakeSignCertificatePolicy signCertificatePolicy = new FakeSignCertificatePolicy();
    public final FakeOcspResolver ocspResolver = new FakeOcspResolver();
    public final FakeIso15118ExiProcessor iso15118ExiProcessor = new FakeIso15118ExiProcessor();

    public final ObjectMapper objectMapper;
    public final OcppWebSocketVerticle verticle;
    public final OcppSessionManager sessionManager;
    public final OcppStationCommandSender commandSender;
    public final com.evlibre.server.core.domain.v16.ports.outbound.Ocpp16StationCommandSender commandSender16;
    public final com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender commandSender201;

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
        AuthorizeUseCase authorize = new AuthorizeUseCase(authRepo, transactionRepo, timeProvider);
        StartTransactionUseCase startTransaction = new StartTransactionUseCase(authorize, transactionRepo, stationRepo,
                new FakeReservationRepository());
        StopTransactionUseCase stopTransaction = new StopTransactionUseCase(transactionRepo, authorize);
        HandleMeterValuesUseCase handleMeterValues = new HandleMeterValuesUseCase(eventLog);
        HandleTransactionEventUseCase handleTransactionEvent = new HandleTransactionEventUseCase(eventLog);
        HandleDataTransferUseCase handleDataTransfer = new HandleDataTransferUseCase(eventLog);
        HandleDiagnosticsStatusUseCase handleDiagnosticsStatus = new HandleDiagnosticsStatusUseCase(eventLog);
        HandleFirmwareStatusUseCase handleFirmwareStatus = new HandleFirmwareStatusUseCase(eventLog);

        // v2.0.1 use case siblings — wired to the same fakes so behavior matches v1.6 paths.
        RegisterStationUseCaseV201 registerStation201 = new RegisterStationUseCaseV201(
                tenantRepo, stationRepo, eventLog, timeProvider, 900, (t, s) -> {});
        HandleHeartbeatUseCaseV201 handleHeartbeat201 = new HandleHeartbeatUseCaseV201(
                stationRepo, timeProvider, (t, s) -> {});
        HandleStatusNotificationUseCaseV201 handleStatusNotification201 = new HandleStatusNotificationUseCaseV201(eventLog);
        AuthorizeUseCaseV201 authorize201 = new AuthorizeUseCaseV201(authRepo, transactionRepo, timeProvider);
        HandleMeterValuesUseCaseV201 handleMeterValues201 = new HandleMeterValuesUseCaseV201(eventLog);
        HandleNotifyReportUseCaseV201 handleNotifyReport201 = new HandleNotifyReportUseCaseV201(
                deviceModelRepo, notifyReportCompletion);
        HandleNotifyMonitoringReportUseCaseV201 handleNotifyMonitoringReport201 =
                new HandleNotifyMonitoringReportUseCaseV201(monitorRepo, notifyMonitoringReportCompletion);
        HandleNotifyCustomerInformationUseCaseV201 handleNotifyCustomerInformation201 =
                new HandleNotifyCustomerInformationUseCaseV201(customerInformationSink);
        HandleNotifyDisplayMessagesUseCaseV201 handleNotifyDisplayMessages201 =
                new HandleNotifyDisplayMessagesUseCaseV201(displayMessagesSink);
        HandleReportChargingProfilesUseCaseV201 handleReportChargingProfiles201 =
                new HandleReportChargingProfilesUseCaseV201(reportChargingProfilesSink);
        HandleNotifyChargingLimitUseCaseV201 handleNotifyChargingLimit201 =
                new HandleNotifyChargingLimitUseCaseV201(chargingLimitSink);
        HandleClearedChargingLimitUseCaseV201 handleClearedChargingLimit201 =
                new HandleClearedChargingLimitUseCaseV201(chargingLimitSink);
        HandleNotifyEVChargingNeedsUseCaseV201 handleNotifyEVChargingNeeds201 =
                new HandleNotifyEVChargingNeedsUseCaseV201(evChargingSink);
        HandleNotifyEVChargingScheduleUseCaseV201 handleNotifyEVChargingSchedule201 =
                new HandleNotifyEVChargingScheduleUseCaseV201(evChargingSink);
        HandleFirmwareStatusNotificationUseCaseV201 handleFirmwareStatus201 =
                new HandleFirmwareStatusNotificationUseCaseV201(firmwareStatusSink);
        HandleLogStatusNotificationUseCaseV201 handleLogStatus201 =
                new HandleLogStatusNotificationUseCaseV201(logStatusSink);
        HandleNotifyEventUseCaseV201 handleNotifyEvent201 =
                new HandleNotifyEventUseCaseV201(notifyEventSink);
        HandlePublishFirmwareStatusNotificationUseCaseV201 handlePublishFirmwareStatus201 =
                new HandlePublishFirmwareStatusNotificationUseCaseV201(publishFirmwareStatusSink);
        HandleSecurityEventNotificationUseCaseV201 handleSecurityEvent201 =
                new HandleSecurityEventNotificationUseCaseV201(securityEventSink);
        HandleSignCertificateUseCaseV201 handleSignCertificate201 =
                new HandleSignCertificateUseCaseV201(signCertificatePolicy);
        HandleGetCertificateStatusUseCaseV201 handleGetCertificateStatus201 =
                new HandleGetCertificateStatusUseCaseV201(ocspResolver);
        HandleGet15118EVCertificateUseCaseV201 handleGet15118EVCertificate201 =
                new HandleGet15118EVCertificateUseCaseV201(iso15118ExiProcessor);
        // v201 DataTransfer allow-list mirrors v16: empty by default, so incoming requests
        // hit the UnknownVendorId branch unless tests specifically rewire a known vendor.
        HandleDataTransferUseCaseV201 handleDataTransfer201 = new HandleDataTransferUseCaseV201(eventLog);

        // OCPP infrastructure
        OcppMessageCodec codec = new OcppMessageCodec(objectMapper);
        OcppSchemaValidator schemaValidator = new OcppSchemaValidator();
        OcppMessageDispatcher dispatcher = new OcppMessageDispatcher();
        sessionManager = new OcppSessionManager();
        OcppProtocolNegotiator negotiator = new OcppProtocolNegotiator();
        OcppPendingCallManager pendingCallManager = new OcppPendingCallManager(schemaValidator);
        commandSender = new OcppStationCommandSender(sessionManager, codec, pendingCallManager, objectMapper, schemaValidator);
        commandSender16 = commandSender.v16();
        commandSender201 = commandSender.v201();

        // Register OCPP 1.6 handlers
        dispatcher.registerHandler(OcppProtocol.OCPP_16, "BootNotification",
                new BootNotificationHandler16(registerStation, null, sessionManager, objectMapper));
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

        // Register OCPP 2.0.1 handlers — wired to the v201 use case siblings.
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "BootNotification",
                new BootNotificationHandler201(registerStation201, null, sessionManager, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "Heartbeat",
                new HeartbeatHandler201(handleHeartbeat201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "StatusNotification",
                new StatusNotificationHandler201(handleStatusNotification201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "Authorize",
                new AuthorizeHandler201(authorize201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "TransactionEvent",
                new TransactionEventHandler201(handleTransactionEvent, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "MeterValues",
                new MeterValuesHandler201(handleMeterValues201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "NotifyReport",
                new NotifyReportHandler201(handleNotifyReport201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "NotifyMonitoringReport",
                new NotifyMonitoringReportHandler201(handleNotifyMonitoringReport201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "NotifyCustomerInformation",
                new NotifyCustomerInformationHandler201(handleNotifyCustomerInformation201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "NotifyDisplayMessages",
                new NotifyDisplayMessagesHandler201(handleNotifyDisplayMessages201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "DataTransfer",
                new DataTransferHandler201(handleDataTransfer201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "ReportChargingProfiles",
                new ReportChargingProfilesHandler201(handleReportChargingProfiles201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "NotifyChargingLimit",
                new NotifyChargingLimitHandler201(handleNotifyChargingLimit201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "ClearedChargingLimit",
                new ClearedChargingLimitHandler201(handleClearedChargingLimit201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "NotifyEVChargingNeeds",
                new NotifyEVChargingNeedsHandler201(handleNotifyEVChargingNeeds201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "NotifyEVChargingSchedule",
                new NotifyEVChargingScheduleHandler201(handleNotifyEVChargingSchedule201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "FirmwareStatusNotification",
                new FirmwareStatusNotificationHandler201(handleFirmwareStatus201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "LogStatusNotification",
                new LogStatusNotificationHandler201(handleLogStatus201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "NotifyEvent",
                new NotifyEventHandler201(handleNotifyEvent201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "PublishFirmwareStatusNotification",
                new PublishFirmwareStatusNotificationHandler201(handlePublishFirmwareStatus201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "SecurityEventNotification",
                new SecurityEventNotificationHandler201(handleSecurityEvent201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "SignCertificate",
                new SignCertificateHandler201(handleSignCertificate201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "GetCertificateStatus",
                new GetCertificateStatusHandler201(handleGetCertificateStatus201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "Get15118EVCertificate",
                new Get15118EVCertificateHandler201(handleGet15118EVCertificate201, objectMapper));

        verticle = new OcppWebSocketVerticle(0, 60, codec, schemaValidator, dispatcher, sessionManager, negotiator, pendingCallManager, (t, s) -> {}, handleHeartbeat);
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
     * Per OCPP 1.6 §4.2 the CP must send BootNotification before anything else. This
     * helper auto-boots (on the same connection) whenever the caller's message is not
     * itself a BootNotification, so unit tests can focus on a single action.
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

        boolean isBoot = message.contains("\"BootNotification\"");

        client.connect(options).onComplete(ar -> {
            if (ar.failed()) {
                future.completeExceptionally(ar.cause());
                return;
            }
            var ws = ar.result();
            if (isBoot) {
                ws.textMessageHandler(msg -> {
                    try {
                        JsonNode parsed = objectMapper.readTree(msg);
                        ws.close().onComplete(closeAr -> future.complete(parsed));
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
                ws.writeTextMessage(message);
            } else {
                String bootMsg = "ocpp1.6".equals(subProtocol)
                        ? OcppMessages.bootNotification16("prelim-boot", "ABB", "Terra AC")
                        : OcppMessages.bootNotification201("prelim-boot", "ABB", "Terra AC");
                boolean[] bootSeen = {false};
                ws.textMessageHandler(msg -> {
                    try {
                        JsonNode parsed = objectMapper.readTree(msg);
                        if (!bootSeen[0]) {
                            bootSeen[0] = true;
                            ws.writeTextMessage(message);
                            return;
                        }
                        ws.close().onComplete(closeAr -> future.complete(parsed));
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
                ws.writeTextMessage(bootMsg);
            }
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
