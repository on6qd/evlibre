package com.evlibre.server;

import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort;
import com.evlibre.server.core.domain.shared.ports.outbound.TenantRepositoryPort;
import com.evlibre.server.core.domain.v16.ports.outbound.AuthorizationRepositoryPort;
import com.evlibre.server.core.domain.v16.ports.outbound.TransactionRepositoryPort;
import io.vertx.core.Vertx;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handle returned by {@link Application#start(com.evlibre.server.config.ServerConfig)}.
 *
 * Exposes the running {@link Vertx} plus the actual bound OCPP / Web UI ports and
 * the repositories acceptance tests assert on (see TESTPLAN.md §5). Callers own
 * the lifecycle and must {@link #close()} when done.
 */
public record AppHandle(
        Vertx vertx,
        int ocppPort,
        int webUiPort,
        TenantRepositoryPort tenantRepository,
        StationRepositoryPort stationRepository,
        TransactionRepositoryPort transactionRepository,
        AuthorizationRepositoryPort authorizationRepository,
        OcppEventLogPort eventLog) implements AutoCloseable {

    @Override
    public void close() {
        try {
            vertx.close().toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while closing Vertx", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException("Failed to close Vertx cleanly", e);
        }
    }
}
