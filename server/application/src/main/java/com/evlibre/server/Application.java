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
import com.evlibre.server.core.usecases.v201.GetBaseReportUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleDataTransferUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleHeartbeatUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleMeterValuesUseCaseV201;
import com.evlibre.server.core.domain.v201.dto.GenericStatus;
import com.evlibre.server.core.domain.v201.dto.NotifyEVChargingNeedsStatus;
import com.evlibre.server.core.usecases.v201.HandleClearedChargingLimitUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleFirmwareStatusNotificationUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleLogStatusNotificationUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleNotifyChargingLimitUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleNotifyEventUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandlePublishFirmwareStatusNotificationUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleNotifyEVChargingNeedsUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleNotifyEVChargingScheduleUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleNotifyReportUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleReportChargingProfilesUseCaseV201;
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
        HandleDataTransferUseCaseV201 handleDataTransfer201 = new HandleDataTransferUseCaseV201(eventLog);
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

        // CSMS-initiated v2.0.1 use cases
        GetBaseReportUseCaseV201 getBaseReport = new GetBaseReportUseCaseV201(commandSender.v201());

        // NotifyReport aggregation — buffers multi-frame reports per requestId, commits
        // on tbc=false. Completion port is a no-op until a subscriber wires itself in.
        HandleNotifyReportUseCaseV201 handleNotifyReport = new HandleNotifyReportUseCaseV201(
                deviceModelRepo,
                (t, s, requestId) -> log.info("NotifyReport complete for station {} (requestId={})",
                        s.value(), requestId));

        // ReportChargingProfiles pass-through: no persistence target yet, so the
        // sink just logs. A subscriber can swap in when a charging-profile repo lands.
        HandleReportChargingProfilesUseCaseV201 handleReportChargingProfiles =
                new HandleReportChargingProfilesUseCaseV201(
                        (t, s, requestId, source, evseId, profiles, tbc) ->
                                log.info("ReportChargingProfiles frame from {} (requestId={}, source={}, evseId={}, profiles={}, tbc={})",
                                        s.value(), requestId, source, evseId, profiles.size(), tbc));

        // Charging-limit notifications from the station (external EMS/SO/CSO imposed/cleared a limit).
        // No persistence yet; sinks just log.
        HandleNotifyChargingLimitUseCaseV201 handleNotifyChargingLimit =
                new HandleNotifyChargingLimitUseCaseV201(
                        (t, s, evseId, limit, schedules) ->
                                log.info("NotifyChargingLimit from {} (evseId={}, source={}, gridCritical={}, schedules={})",
                                        s.value(), evseId, limit.chargingLimitSource(), limit.isGridCritical(),
                                        schedules == null ? 0 : schedules.size()));
        HandleClearedChargingLimitUseCaseV201 handleClearedChargingLimit =
                new HandleClearedChargingLimitUseCaseV201(
                        (t, s, source, evseId) ->
                                log.info("ClearedChargingLimit from {} (source={}, evseId={})",
                                        s.value(), source, evseId));

        // ISO 15118 EV-originated smart-charging acks. Default policies return Accepted
        // so stations aren't blocked until real schedule synthesis lands.
        HandleNotifyEVChargingNeedsUseCaseV201 handleNotifyEVChargingNeeds =
                new HandleNotifyEVChargingNeedsUseCaseV201(
                        (t, s, evseId, maxScheduleTuples, needs) -> {
                            log.info("NotifyEVChargingNeeds from {} (evseId={}, mode={}, departureTime={})",
                                    s.value(), evseId, needs.requestedEnergyTransfer(), needs.departureTime());
                            return NotifyEVChargingNeedsStatus.ACCEPTED;
                        });
        HandleNotifyEVChargingScheduleUseCaseV201 handleNotifyEVChargingSchedule =
                new HandleNotifyEVChargingScheduleUseCaseV201(
                        (t, s, timeBase, evseId, schedule) -> {
                            log.info("NotifyEVChargingSchedule from {} (evseId={}, timeBase={}, periods={})",
                                    s.value(), evseId, timeBase, schedule.chargingSchedulePeriod().size());
                            return GenericStatus.ACCEPTED;
                        });

        // Firmware-update progress notifications from the station (L01). No persistence
        // yet — the sink just logs.
        HandleFirmwareStatusNotificationUseCaseV201 handleFirmwareStatus201 =
                new HandleFirmwareStatusNotificationUseCaseV201(
                        (t, s, status, requestId) ->
                                log.info("FirmwareStatusNotification from {} (status={}, requestId={})",
                                        s.value(), status, requestId));

        // Log-upload progress notifications from the station (N01). No persistence
        // yet — the sink just logs.
        HandleLogStatusNotificationUseCaseV201 handleLogStatus201 =
                new HandleLogStatusNotificationUseCaseV201(
                        (t, s, status, requestId) ->
                                log.info("LogStatusNotification from {} (status={}, requestId={})",
                                        s.value(), status, requestId));

        // Variable / monitor event batches from the station (N07/N08). No persistence
        // yet — the sink just logs the frame size.
        HandleNotifyEventUseCaseV201 handleNotifyEvent201 =
                new HandleNotifyEventUseCaseV201(
                        (t, s, generatedAt, seqNo, tbc, events) ->
                                log.info("NotifyEvent from {} (seqNo={}, tbc={}, events={})",
                                        s.value(), seqNo, tbc, events.size()));

        // Local-firmware publish progress from a Local Controller (L03). No persistence
        // yet — the sink just logs.
        HandlePublishFirmwareStatusNotificationUseCaseV201 handlePublishFirmwareStatus201 =
                new HandlePublishFirmwareStatusNotificationUseCaseV201(
                        (t, s, status, locations, requestId) ->
                                log.info("PublishFirmwareStatusNotification from {} (status={}, requestId={}, locations={})",
                                        s.value(), status, requestId, locations.size()));

        // Post-boot actions (GetConfiguration for 1.6, GetBaseReport for 2.0.1)
        PostBootActionService postBootActionService = new PostBootActionService(
                commandSender.v16(), getBaseReport, stationConfigRepo);

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
                new NotifyReportHandler201(handleNotifyReport, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "DataTransfer",
                new DataTransferHandler201(handleDataTransfer201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "ReportChargingProfiles",
                new ReportChargingProfilesHandler201(handleReportChargingProfiles, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "NotifyChargingLimit",
                new NotifyChargingLimitHandler201(handleNotifyChargingLimit, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "ClearedChargingLimit",
                new ClearedChargingLimitHandler201(handleClearedChargingLimit, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "NotifyEVChargingNeeds",
                new NotifyEVChargingNeedsHandler201(handleNotifyEVChargingNeeds, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "NotifyEVChargingSchedule",
                new NotifyEVChargingScheduleHandler201(handleNotifyEVChargingSchedule, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "FirmwareStatusNotification",
                new FirmwareStatusNotificationHandler201(handleFirmwareStatus201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "LogStatusNotification",
                new LogStatusNotificationHandler201(handleLogStatus201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "NotifyEvent",
                new NotifyEventHandler201(handleNotifyEvent201, objectMapper));
        dispatcher.registerHandler(OcppProtocol.OCPP_201, "PublishFirmwareStatusNotification",
                new PublishFirmwareStatusNotificationHandler201(handlePublishFirmwareStatus201, objectMapper));

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
