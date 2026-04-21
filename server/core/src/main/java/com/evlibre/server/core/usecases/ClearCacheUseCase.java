package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.ClearCachePort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ClearCacheUseCase implements ClearCachePort {

    private static final Logger log = LoggerFactory.getLogger(ClearCacheUseCase.class);

    private final StationCommandSender commandSender;

    public ClearCacheUseCase(StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> clearCache(TenantId tenantId, ChargePointIdentity stationIdentity) {
        log.info("Sending ClearCache to {} (tenant: {})", stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "ClearCache", Collections.emptyMap())
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("ClearCache response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }
}
