package com.evlibre.server;

import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.*;
import com.evlibre.server.adapter.ocpp.handler.v16.*;
import com.evlibre.server.adapter.ocpp.handler.v201.*;
import com.evlibre.server.adapter.persistence.h2.*;
import com.evlibre.server.adapter.persistence.inmemory.*;
import com.evlibre.server.config.ConfigLoader;
import com.evlibre.server.config.ServerConfig;
import com.evlibre.server.core.domain.model.AuthorizationStatus;
import com.evlibre.server.core.domain.model.Tenant;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.*;
import com.evlibre.server.core.usecases.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

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

        // Use cases
        RegisterStationUseCase registerStation = new RegisterStationUseCase(
                tenantRepo, stationRepo, eventLog, timeProvider,
                config.ocpp().heartbeatInterval());
        HandleHeartbeatUseCase handleHeartbeat = new HandleHeartbeatUseCase(stationRepo, timeProvider);
        HandleStatusNotificationUseCase handleStatusNotification = new HandleStatusNotificationUseCase(eventLog);
        AuthorizeUseCase authorize = new AuthorizeUseCase(authorizationRepo);
        StartTransactionUseCase startTransaction = new StartTransactionUseCase(authorize, transactionRepo, stationRepo);
        StopTransactionUseCase stopTransaction = new StopTransactionUseCase(transactionRepo);
        HandleMeterValuesUseCase handleMeterValues = new HandleMeterValuesUseCase(eventLog);
        HandleTransactionEventUseCase handleTransactionEvent = new HandleTransactionEventUseCase(eventLog);

        // OCPP WebSocket components
        OcppMessageCodec codec = new OcppMessageCodec(objectMapper);
        OcppSchemaValidator schemaValidator = new OcppSchemaValidator();
        OcppMessageDispatcher dispatcher = new OcppMessageDispatcher();
        OcppSessionManager sessionManager = new OcppSessionManager();
        OcppProtocolNegotiator protocolNegotiator = new OcppProtocolNegotiator();

        // Register OCPP 1.6 handlers
        dispatcher.registerHandler(OcppProtocol.OCPP_16, "BootNotification",
                new BootNotificationHandler16(registerStation, objectMapper));
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

        // Register OCPP 2.0.1 handlers
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "BootNotification",
                new BootNotificationHandler201(registerStation, objectMapper));
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

        // CSMS -> CS command support
        OcppPendingCallManager pendingCallManager = new OcppPendingCallManager();
        OcppStationCommandSender commandSender = new OcppStationCommandSender(
                sessionManager, codec, pendingCallManager, objectMapper);

        // Create and deploy verticle
        OcppWebSocketVerticle ocppVerticle = new OcppWebSocketVerticle(
                config.ocpp().websocketPort(), codec, schemaValidator,
                dispatcher, sessionManager, protocolNegotiator, pendingCallManager);

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(ocppVerticle).onSuccess(id -> {
            log.info("OCPP server started on port {} (database: {})",
                    config.ocpp().websocketPort(), dbType);
        }).onFailure(err -> {
            log.error("Failed to start OCPP server: {}", err.getMessage());
            vertx.close();
        });
    }
}
