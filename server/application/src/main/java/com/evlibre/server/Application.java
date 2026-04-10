package com.evlibre.server;

import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.*;
import com.evlibre.server.adapter.ocpp.handler.v16.BootNotificationHandler16;
import com.evlibre.server.adapter.ocpp.handler.v16.HeartbeatHandler16;
import com.evlibre.server.adapter.ocpp.handler.v16.StatusNotificationHandler16;
import com.evlibre.server.adapter.persistence.inmemory.InMemoryOcppEventLog;
import com.evlibre.server.adapter.persistence.inmemory.InMemoryStationRepository;
import com.evlibre.server.adapter.persistence.inmemory.InMemoryTenantRepository;
import com.evlibre.server.adapter.persistence.inmemory.SystemTimeProvider;
import com.evlibre.server.config.ConfigLoader;
import com.evlibre.server.config.ServerConfig;
import com.evlibre.server.core.domain.model.Tenant;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.usecases.HandleHeartbeatUseCase;
import com.evlibre.server.core.usecases.HandleStatusNotificationUseCase;
import com.evlibre.server.core.usecases.RegisterStationUseCase;
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

        // Persistence adapters
        InMemoryTenantRepository tenantRepo = new InMemoryTenantRepository();
        InMemoryStationRepository stationRepo = new InMemoryStationRepository();
        InMemoryOcppEventLog eventLog = new InMemoryOcppEventLog();
        SystemTimeProvider timeProvider = new SystemTimeProvider();

        // Seed demo tenant
        tenantRepo.save(Tenant.builder()
                .id(UUID.randomUUID())
                .tenantId(new TenantId("demo-tenant"))
                .companyName("Demo Company")
                .createdAt(Instant.now())
                .build());
        log.info("Seeded demo-tenant");

        // Use cases
        RegisterStationUseCase registerStation = new RegisterStationUseCase(
                tenantRepo, stationRepo, eventLog, timeProvider,
                config.ocpp().heartbeatInterval());
        HandleHeartbeatUseCase handleHeartbeat = new HandleHeartbeatUseCase(stationRepo, timeProvider);
        HandleStatusNotificationUseCase handleStatusNotification = new HandleStatusNotificationUseCase(eventLog);

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

        // Create and deploy verticle
        OcppWebSocketVerticle ocppVerticle = new OcppWebSocketVerticle(
                config.ocpp().websocketPort(), codec, schemaValidator,
                dispatcher, sessionManager, protocolNegotiator);

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(ocppVerticle).onSuccess(id -> {
            log.info("OCPP server started on port {}", config.ocpp().websocketPort());
        }).onFailure(err -> {
            log.error("Failed to start OCPP server: {}", err.getMessage());
            vertx.close();
        });
    }
}
