package com.evlibre.server.adapter.webui;

import com.evlibre.server.adapter.webui.handlers.DashboardHandler;
import com.evlibre.server.adapter.webui.handlers.ErrorHandler;
import com.evlibre.server.adapter.webui.handlers.StationsHandler;
import com.evlibre.server.adapter.webui.handlers.TenantContextExtractor;
import com.evlibre.server.core.domain.ports.outbound.StationRepositoryPort;
import com.evlibre.server.core.domain.ports.outbound.TenantRepositoryPort;
import com.evlibre.server.core.domain.ports.outbound.TransactionRepositoryPort;
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
    private final int port;
    private HttpServer httpServer;

    public WebUiVerticle(TenantRepositoryPort tenantRepository,
                         StationRepositoryPort stationRepository,
                         TransactionRepositoryPort transactionRepository,
                         int port) {
        this.tenantRepository = tenantRepository;
        this.stationRepository = stationRepository;
        this.transactionRepository = transactionRepository;
        this.port = port;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        TenantContextExtractor contextExtractor = new TenantContextExtractor(tenantRepository);
        DashboardHandler dashboardHandler = new DashboardHandler(vertx, stationRepository);
        StationsHandler stationsHandler = new StationsHandler(vertx, stationRepository);
        ErrorHandler errorHandler = new ErrorHandler();

        Router router = Router.router(vertx);

        router.route("/static/*").handler(StaticHandler.create("static"));
        router.route().handler(BodyHandler.create());

        // Multi-tenant routes
        router.get("/ui/:tenantId/dashboard")
                .handler(ctx -> contextExtractor.extractAndValidate(ctx, dashboardHandler::showDashboard));
        router.get("/ui/:tenantId/stations")
                .handler(ctx -> contextExtractor.extractAndValidate(ctx, stationsHandler::listStations));

        // Root
        router.get("/").handler(ctx -> {
            ctx.response()
                    .putHeader("Content-Type", "text/html; charset=UTF-8")
                    .end("<html><body><h1>evlibre</h1><p>Navigate to /ui/{tenantId}/dashboard</p></body></html>");
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
}
