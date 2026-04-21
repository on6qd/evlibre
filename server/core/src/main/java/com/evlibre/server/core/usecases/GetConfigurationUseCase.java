package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.v16.model.StationConfigurationKey;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.GetConfigurationPort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import com.evlibre.server.core.domain.v16.ports.outbound.StationConfigurationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class GetConfigurationUseCase implements GetConfigurationPort {

    private static final Logger log = LoggerFactory.getLogger(GetConfigurationUseCase.class);

    private final StationCommandSender commandSender;
    private final StationConfigurationPort configurationPort;

    public GetConfigurationUseCase(StationCommandSender commandSender,
                                    StationConfigurationPort configurationPort) {
        this.commandSender = Objects.requireNonNull(commandSender);
        this.configurationPort = Objects.requireNonNull(configurationPort);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<List<StationConfigurationKey>> getConfiguration(TenantId tenantId,
                                                                              ChargePointIdentity stationIdentity,
                                                                              List<String> keys) {
        log.info("Sending GetConfiguration to {} (tenant: {}, keys: {})",
                stationIdentity.value(), tenantId.value(), keys);

        Map<String, Object> payload = keys != null && !keys.isEmpty()
                ? Map.of("key", keys)
                : Collections.emptyMap();

        return commandSender.sendCommand(tenantId, stationIdentity, "GetConfiguration", payload)
                .thenApply(response -> {
                    List<Map<String, Object>> configKeys =
                            (List<Map<String, Object>>) response.get("configurationKey");
                    if (configKeys == null) {
                        log.info("GetConfiguration response from {} has no configuration keys",
                                stationIdentity.value());
                        return Collections.<StationConfigurationKey>emptyList();
                    }

                    List<StationConfigurationKey> result = new ArrayList<>();
                    for (Map<String, Object> entry : configKeys) {
                        String key = (String) entry.get("key");
                        String value = entry.get("value") != null ? entry.get("value").toString() : null;
                        boolean readonly = Boolean.TRUE.equals(entry.get("readonly"));
                        result.add(new StationConfigurationKey(key, value, readonly));
                    }

                    configurationPort.saveConfiguration(tenantId, stationIdentity, result);
                    log.info("Stored {} configuration keys for {}", result.size(), stationIdentity.value());
                    return result;
                });
    }
}
