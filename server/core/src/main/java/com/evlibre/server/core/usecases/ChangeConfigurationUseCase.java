package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.StationConfigurationKey;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.ChangeConfigurationPort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import com.evlibre.server.core.domain.ports.outbound.StationConfigurationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ChangeConfigurationUseCase implements ChangeConfigurationPort {

    private static final Logger log = LoggerFactory.getLogger(ChangeConfigurationUseCase.class);

    private final StationCommandSender commandSender;
    private final StationConfigurationPort configurationPort;

    public ChangeConfigurationUseCase(StationCommandSender commandSender,
                                       StationConfigurationPort configurationPort) {
        this.commandSender = Objects.requireNonNull(commandSender);
        this.configurationPort = Objects.requireNonNull(configurationPort);
    }

    @Override
    public CompletableFuture<CommandResult> changeConfiguration(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                                  String key, String value) {
        log.info("Sending ChangeConfiguration to {} (tenant: {}, key: {})",
                stationIdentity.value(), tenantId.value(), key);

        Map<String, Object> payload = Map.of("key", key, "value", value);
        return commandSender.sendCommand(tenantId, stationIdentity, "ChangeConfiguration", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("ChangeConfiguration response from {}: {}", stationIdentity.value(), status);

                    if ("Accepted".equals(status)) {
                        configurationPort.updateConfigurationKey(tenantId, stationIdentity,
                                new StationConfigurationKey(key, value, false));
                    }
                    return new CommandResult(status, response);
                });
    }
}
