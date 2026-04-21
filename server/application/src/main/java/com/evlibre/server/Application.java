package com.evlibre.server;

import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.*;
import com.evlibre.server.adapter.ocpp.handler.v16.*;
import com.evlibre.server.adapter.ocpp.handler.v201.*;
import com.evlibre.server.adapter.persistence.inmemory.InMemoryDeviceModelRepository;
import com.evlibre.server.adapter.persistence.inmemory.InMemoryStationConfigurationRepository;
import com.evlibre.server.adapter.webui.EventBusStationEventPublisher;
import com.evlibre.server.adapter.webui.WebUiVerticle;
import com.evlibre.server.adapter.persistence.h2.*;
import com.evlibre.server.adapter.persistence.inmemory.*;
import com.evlibre.server.config.ConfigLoader;
import com.evlibre.server.config.ServerConfig;
import com.evlibre.server.core.domain.v16.model.AuthorizationStatus;
import com.evlibre.server.core.domain.shared.model.Tenant;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.*;
import com.evlibre.server.core.domain.v16.ports.outbound.*;
import com.evlibre.server.core.domain.v201.ports.outbound.*;
import com.evlibre.server.core.usecases.v16.*;
import com.evlibre.server.core.usecases.v201.AuthorizeUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleHeartbeatUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleMeterValuesUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleStatusNotificationUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleTransactionEventUseCase;
import com.evlibre.server.core.usecases.v201.RegisterStationUseCaseV201;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;
import com.evlibre.server.core.domain.shared.ports.outbound.TimeProvider;
import com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort;
import com.evlibre.server.core.domain.shared.ports.outbound.TenantRepositoryPort;
import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import com.evlibre.server.core.domain.v16.ports.outbound.TransactionRepositoryPort;
import com.evlibre.server.core.domain.v16.ports.outbound.AuthorizationRepositoryPort;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        ServerConfig config = ConfigLoader.load(args);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Persistence adapters — selected by config
        TenantRepositoryPort tenantRepo;
        StationRepositoryPort stationRepo;
        OcppEventLogPort eventLog;
        TransactionRepositoryPort transactionRepo;
        AuthorizationRepositoryPort authorizationRepo;
        TimeProvider timeProvider = new SystemTimeProvider();

        String dbType = config.database().type();
        if ("h2-file".equals(dbType)) {
            log.info("Using H2 file-based persistence: {}", config.database().jdbcUrl());
            var dbConfig = config.database();
            H2DatabaseManager db = new H2DatabaseManager(
                    dbConfig.jdbcUrl(), dbConfig.username(), dbConfig.password(),
                    dbConfig.poolSize(), dbConfig.runMigrations());

            tenantRepo = new H2TenantRepository(db);
            stationRepo = new H2StationRepository(db);
            eventLog = new H2OcppEventLog(db);
            transactionRepo = new H2TransactionRepository(db);
            authorizationRepo = new H2AuthorizationRepository(db);
            // Demo data seeded by Flyway migration V6
        } else {
            log.info("Using in-memory persistence");
            var inMemTenantRepo = new InMemoryTenantRepository();
            var inMemStationRepo = new InMemoryStationRepository();
            var inMemEventLog = new InMemoryOcppEventLog();
            var inMemTransactionRepo = new InMemoryTransactionRepository();
            var inMemAuthorizationRepo = new InMemoryAuthorizationRepository();

            // Seed demo data for in-memory mode
            inMemTenantRepo.save(Tenant.builder()
                    .id(UUID.randomUUID())
                    .tenantId(new TenantId("demo-tenant"))
                    .companyName("Demo Company")
                    .createdAt(Instant.now())
                    .build());
            inMemAuthorizationRepo.addAuthorization(
                    new TenantId("demo-tenant"), "TAG001", AuthorizationStatus.ACCEPTED);
            log.info("Seeded demo-tenant + TAG001");

            tenantRepo = inMemTenantRepo;
            stationRepo = inMemStationRepo;
            eventLog = inMemEventLog;
            transactionRepo = inMemTransactionRepo;
            authorizationRepo = inMemAuthorizationRepo;
        }

        // Vert.x instance (created early so EventBus is available for use cases)
        Vertx vertx = Vertx.vertx();

        // Station event publisher (pushes updates to Web UI via EventBus)
        EventBusStationEventPublisher stationEventPublisher = new EventBusStationEventPublisher(vertx);

        // Use cases
        RegisterStationUseCase registerStation = new RegisterStationUseCase(
                tenantRepo, stationRepo, eventLog, timeProvider,
                config.ocpp().heartbeatInterval(), stationEventPublisher);
        HandleHeartbeatUseCase handleHeartbeat = new HandleHeartbeatUseCase(stationRepo, timeProvider,
                stationEventPublisher);
        HandleStatusNotificationUseCase handleStatusNotification = new HandleStatusNotificationUseCase(eventLog);
        AuthorizeUseCase authorize = new AuthorizeUseCase(authorizationRepo, transactionRepo, timeProvider);
        var reservationRepo = new InMemoryReservationRepository();
        StartTransactionUseCase startTransaction = new StartTransactionUseCase(authorize, transactionRepo, stationRepo, reservationRepo);
        StopTransactionUseCase stopTransaction = new StopTransactionUseCase(transactionRepo, authorize);
        HandleMeterValuesUseCase handleMeterValues = new HandleMeterValuesUseCase(eventLog);
        HandleTransactionEventUseCase handleTransactionEvent = new HandleTransactionEventUseCase(eventLog);

        // v2.0.1 use cases (siblings of the v1.6 ones above; kept separate per the
        // strict 1.6 / 2.0.1 separation rule — v201 handlers must not reach into
        // v16 use cases even when the business logic is currently identical).
        RegisterStationUseCaseV201 registerStation201 = new RegisterStationUseCaseV201(
                tenantRepo, stationRepo, eventLog, timeProvider,
                config.ocpp().heartbeatInterval(), stationEventPublisher);
        HandleHeartbeatUseCaseV201 handleHeartbeat201 = new HandleHeartbeatUseCaseV201(stationRepo, timeProvider,
                stationEventPublisher);
        HandleStatusNotificationUseCaseV201 handleStatusNotification201 = new HandleStatusNotificationUseCaseV201(eventLog);
        AuthorizeUseCaseV201 authorize201 = new AuthorizeUseCaseV201(authorizationRepo, transactionRepo, timeProvider);
        HandleMeterValuesUseCaseV201 handleMeterValues201 = new HandleMeterValuesUseCaseV201(eventLog);
        HandleDataTransferUseCase handleDataTransfer = new HandleDataTransferUseCase(eventLog);
        HandleDiagnosticsStatusUseCase handleDiagnosticsStatus = new HandleDiagnosticsStatusUseCase(eventLog);
        HandleFirmwareStatusUseCase handleFirmwareStatus = new HandleFirmwareStatusUseCase(eventLog);

        // OCPP WebSocket components
        OcppMessageCodec codec = new OcppMessageCodec(objectMapper);
        OcppSchemaValidator schemaValidator = new OcppSchemaValidator();
        OcppMessageDispatcher dispatcher = new OcppMessageDispatcher();
        OcppSessionManager sessionManager = new OcppSessionManager();
        OcppProtocolNegotiator protocolNegotiator = new OcppProtocolNegotiator();

        // CSMS -> CS command support
        OcppPendingCallManager pendingCallManager = new OcppPendingCallManager(schemaValidator);
        OcppStationCommandSender commandSender = new OcppStationCommandSender(
                sessionManager, codec, pendingCallManager, objectMapper, schemaValidator);

        // Station configuration storage
        var stationConfigRepo = new InMemoryStationConfigurationRepository();
        var deviceModelRepo = new InMemoryDeviceModelRepository();

        // Post-boot actions (GetConfiguration for 1.6, GetBaseReport for 2.0.1)
        PostBootActionService postBootActionService = new PostBootActionService(
                commandSender.v16(), commandSender.v201(), stationConfigRepo);

        // Register OCPP 1.6 handlers
        dispatcher.registerHandler(OcppProtocol.OCPP_16, "BootNotification",
                new BootNotificationHandler16(registerStation, postBootActionService, sessionManager, objectMapper));
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
                new BootNotificationHandler201(registerStation201, postBootActionService, sessionManager, objectMapper));
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
                new NotifyReportHandler201(deviceModelRepo, objectMapper));

        // Create and deploy verticle
        OcppWebSocketVerticle ocppVerticle = new OcppWebSocketVerticle(
                config.ocpp().websocketPort(), config.ocpp().pingInterval(),
                codec, schemaValidator,
                dispatcher, sessionManager, protocolNegotiator, pendingCallManager,
                stationEventPublisher, handleHeartbeat);

        // Web UI — wired to the v1.6 sender for now; v2.0.1 remote commands land in a later phase.
        WebUiVerticle webUiVerticle = new WebUiVerticle(
                tenantRepo, stationRepo, transactionRepo, sessionManager,
                commandSender.v16(), config.webui().port());

        vertx.deployVerticle(ocppVerticle).onSuccess(id -> {
            log.info("OCPP server started on port {} (database: {})",
                    config.ocpp().websocketPort(), dbType);
        }).onFailure(err -> {
            log.error("Failed to start OCPP server: {}", err.getMessage());
            vertx.close();
        });

        vertx.deployVerticle(webUiVerticle).onSuccess(id -> {
            log.info("Web UI started on port {}", config.webui().port());
        }).onFailure(err -> {
            log.error("Failed to start Web UI: {}", err.getMessage());
        });
    }
}
