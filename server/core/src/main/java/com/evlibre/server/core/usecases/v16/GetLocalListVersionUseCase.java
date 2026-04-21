package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.inbound.GetLocalListVersionPort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GetLocalListVersionUseCase implements GetLocalListVersionPort {

    private static final Logger log = LoggerFactory.getLogger(GetLocalListVersionUseCase.class);

    private final StationCommandSender commandSender;

    public GetLocalListVersionUseCase(StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> getLocalListVersion(TenantId tenantId,
                                                                  ChargePointIdentity stationIdentity) {
        log.info("Sending GetLocalListVersion to {} (tenant: {})", stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "GetLocalListVersion", Collections.emptyMap())
                .thenApply(response -> {
                    String version = String.valueOf(response.getOrDefault("listVersion", "-1"));
                    log.info("GetLocalListVersion response from {}: version={}", stationIdentity.value(), version);
                    return new CommandResult(version, response);
                });
    }
}
