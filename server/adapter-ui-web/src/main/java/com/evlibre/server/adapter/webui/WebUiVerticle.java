package com.evlibre.server.adapter.webui;

import com.evlibre.server.adapter.ocpp.OcppSessionManager;
import com.evlibre.server.adapter.webui.handlers.DashboardHandler;
import com.evlibre.server.adapter.webui.handlers.ErrorHandler;
import com.evlibre.server.adapter.webui.handlers.SseHandler;
import com.evlibre.server.adapter.webui.handlers.StationCommandHandler;
import com.evlibre.server.adapter.webui.handlers.StationCommandHandlerV201;
import com.evlibre.server.adapter.webui.handlers.StationDetailHandler;
import com.evlibre.server.adapter.webui.handlers.StationsHandler;
import com.evlibre.server.adapter.webui.handlers.TenantContextExtractor;
import com.evlibre.server.core.domain.v16.ports.outbound.Ocpp16StationCommandSender;
import com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort;
import com.evlibre.server.core.domain.shared.ports.outbound.TenantRepositoryPort;
import com.evlibre.server.core.domain.v16.ports.outbound.TransactionRepositoryPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebUiVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(WebUiVerticle.class);

    private final TenantRepositoryPort tenantRepository;
    private final StationRepositoryPort stationRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final OcppSessionManager sessionManager;
    private final Ocpp16StationCommandSender commandSender;
    private final Ocpp201StationCommandSender commandSender201;
    private final int port;
    private HttpServer httpServer;

    public WebUiVerticle(TenantRepositoryPort tenantRepository,
                         StationRepositoryPort stationRepository,
                         TransactionRepositoryPort transactionRepository,
                         OcppSessionManager sessionManager,
                         int port) {
        this(tenantRepository, stationRepository, transactionRepository, sessionManager, null, null, port);
    }

    public WebUiVerticle(TenantRepositoryPort tenantRepository,
                         StationRepositoryPort stationRepository,
                         TransactionRepositoryPort transactionRepository,
                         OcppSessionManager sessionManager,
                         Ocpp16StationCommandSender commandSender,
                         int port) {
        this(tenantRepository, stationRepository, transactionRepository, sessionManager, commandSender, null, port);
    }

    public WebUiVerticle(TenantRepositoryPort tenantRepository,
                         StationRepositoryPort stationRepository,
                         TransactionRepositoryPort transactionRepository,
                         OcppSessionManager sessionManager,
                         Ocpp16StationCommandSender commandSender,
                         Ocpp201StationCommandSender commandSender201,
                         int port) {
        this.tenantRepository = tenantRepository;
        this.stationRepository = stationRepository;
        this.transactionRepository = transactionRepository;
        this.sessionManager = sessionManager;
        this.commandSender = commandSender;
        this.commandSender201 = commandSender201;
        this.port = port;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        TenantContextExtractor contextExtractor = new TenantContextExtractor(tenantRepository);
        DashboardHandler dashboardHandler = new DashboardHandler(vertx, stationRepository, sessionManager);
        StationsHandler stationsHandler = new StationsHandler(vertx, stationRepository, sessionManager);
        StationDetailHandler stationDetailHandler = new StationDetailHandler(vertx, stationRepository, sessionManager);
        SseHandler sseHandler = new SseHandler(vertx, stationRepository, sessionManager);
        ErrorHandler errorHandler = new ErrorHandler();

        Router router = Router.router(vertx);

        router.route("/static/*").handler(StaticHandler.create("static"));
        router.route().handler(BodyHandler.create());

        // Multi-tenant routes
        router.get("/:tenantId/dashboard")
                .handler(ctx -> contextExtractor.extractAndValidate(ctx, dashboardHandler::showDashboard));
        router.get("/:tenantId/stations")
                .handler(ctx -> contextExtractor.extractAndValidate(ctx, stationsHandler::listStations));
        router.get("/:tenantId/stations/:stationId")
                .handler(ctx -> contextExtractor.extractAndValidate(ctx, stationDetailHandler::showStation));
        router.get("/:tenantId/events/stations")
                .handler(ctx -> contextExtractor.extractAndValidate(ctx, sseHandler::streamStationUpdates));

        // Station command endpoints (POST)
        if (commandSender != null) {
            StationCommandHandler cmdHandler = new StationCommandHandler(commandSender);
            router.post("/:tenantId/stations/:stationId/reset")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler::reset));
            router.post("/:tenantId/stations/:stationId/change-availability")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler::changeAvailability));
            router.post("/:tenantId/stations/:stationId/unlock-connector")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler::unlockConnector));
            router.post("/:tenantId/stations/:stationId/clear-cache")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler::clearCache));
            router.post("/:tenantId/stations/:stationId/remote-start")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler::remoteStart));
            router.post("/:tenantId/stations/:stationId/remote-stop")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler::remoteStop));
            router.post("/:tenantId/stations/:stationId/get-diagnostics")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler::getDiagnostics));
            router.post("/:tenantId/stations/:stationId/update-firmware")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler::updateFirmware));
        }
        if (commandSender201 != null) {
            StationCommandHandlerV201 cmdHandler201 = new StationCommandHandlerV201(commandSender201);
            router.post("/:tenantId/stations/:stationId/v201/clear-cache")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler201::clearCache));
            router.post("/:tenantId/stations/:stationId/v201/reset")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler201::reset));
            router.post("/:tenantId/stations/:stationId/v201/change-availability")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler201::changeAvailability));
            router.post("/:tenantId/stations/:stationId/v201/unlock-connector")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler201::unlockConnector));
            router.post("/:tenantId/stations/:stationId/v201/request-start")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler201::requestStartTransaction));
            router.post("/:tenantId/stations/:stationId/v201/request-stop")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler201::requestStopTransaction));
            router.post("/:tenantId/stations/:stationId/v201/get-log")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler201::getLog));
            router.post("/:tenantId/stations/:stationId/v201/update-firmware")
                    .handler(ctx -> contextExtractor.extractAndValidate(ctx, cmdHandler201::updateFirmware));
        }

        // Root
        router.get("/").handler(ctx -> {
            ctx.response()
                    .putHeader("Content-Type", "text/html; charset=UTF-8")
                    .end(buildWelcomePage());
        });

        // Error handling
        router.route().failureHandler(errorHandler::handleError);
        router.route().last().handler(errorHandler::handle404);

        httpServer = vertx.createHttpServer();
        httpServer.requestHandler(router).listen(port, result -> {
            if (result.succeeded()) {
                log.info("Web UI server listening on port {}", actualPort());
                startPromise.complete();
            } else {
                log.error("Failed to start Web UI server on port {}", port, result.cause());
                startPromise.fail(result.cause());
            }
        });
    }

    public int actualPort() {
        return httpServer != null ? httpServer.actualPort() : -1;
    }

    private String buildWelcomePage() {
        return """
                <!DOCTYPE html>
                <html><head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: 'Courier New', monospace;
                        background: #0a0a0a;
                        color: #00ff88;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        margin: 0;
                    }
                    .welcome {
                        border: 1px solid #333;
                        padding: 32px 48px;
                        text-align: center;
                    }
                    h1 { font-size: 24px; letter-spacing: 4px; margin-bottom: 16px; }
                    p { color: #666; font-size: 12px; margin-bottom: 16px; }
                    a {
                        color: #00ff88;
                        border: 1px solid #333;
                        padding: 8px 20px;
                        text-decoration: none;
                        font-size: 11px;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                    }
                    a:hover { border-color: #00ff88; }
                </style>
                </head>
                <body>
                <div class="welcome">
                    <h1>evlibre</h1>
                    <p>open source ev charging platform</p>
                    <a href="/demo-tenant/dashboard">enter &gt;</a>
                </div>
                </body></html>
                """;
    }
}
