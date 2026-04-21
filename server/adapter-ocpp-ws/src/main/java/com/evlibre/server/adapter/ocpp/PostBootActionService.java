package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.core.domain.v16.model.StationConfigurationKey;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import com.evlibre.server.core.domain.v16.ports.outbound.StationConfigurationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Sends post-boot CSMS-initiated messages after a station's BootNotification is accepted.
 * <p>
 * OCPP 1.6: sends GetConfiguration (empty) to retrieve all configuration keys.
 * OCPP 2.0.1: sends GetBaseReport (FullInventory) to retrieve the device model.
 */
public class PostBootActionService {

    private static final Logger log = LoggerFactory.getLogger(PostBootActionService.class);

    private final StationCommandSender commandSender;
    private final StationConfigurationPort configurationPort;

    public PostBootActionService(StationCommandSender commandSender,
                                 StationConfigurationPort configurationPort) {
        this.commandSender = commandSender;
        this.configurationPort = configurationPort;
    }

    public void onBootAccepted(TenantId tenantId, ChargePointIdentity stationIdentity, OcppProtocol protocol) {
        if (protocol == OcppProtocol.OCPP_16) {
            sendGetConfiguration(tenantId, stationIdentity);
        } else if (protocol == OcppProtocol.OCPP_201) {
            sendGetBaseReport(tenantId, stationIdentity);
        }
    }

    private void sendGetConfiguration(TenantId tenantId, ChargePointIdentity stationIdentity) {
        log.info("Sending GetConfiguration to {} (tenant: {})", stationIdentity.value(), tenantId.value());

        commandSender.sendCommand(tenantId, stationIdentity, "GetConfiguration", Collections.emptyMap())
                .thenAccept(response -> handleGetConfigurationResponse(tenantId, stationIdentity, response))
                .exceptionally(error -> {
                    log.warn("GetConfiguration failed for {}: {}", stationIdentity.value(), error.getMessage());
                    return null;
                });
    }

    @SuppressWarnings("unchecked")
    private void handleGetConfigurationResponse(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                 Map<String, Object> response) {
        List<Map<String, Object>> configKeys = (List<Map<String, Object>>) response.get("configurationKey");
        if (configKeys == null) {
            log.info("GetConfiguration response from {} has no configuration keys", stationIdentity.value());
            return;
        }

        List<StationConfigurationKey> keys = new ArrayList<>();
        for (Map<String, Object> entry : configKeys) {
            String key = (String) entry.get("key");
            String value = entry.get("value") != null ? entry.get("value").toString() : null;
            boolean readonly = Boolean.TRUE.equals(entry.get("readonly"));
            keys.add(new StationConfigurationKey(key, value, readonly));
        }

        configurationPort.saveConfiguration(tenantId, stationIdentity, keys);
        log.info("Stored {} configuration keys for {}", keys.size(), stationIdentity.value());
    }

    private void sendGetBaseReport(TenantId tenantId, ChargePointIdentity stationIdentity) {
        log.info("Sending GetBaseReport to {} (tenant: {})", stationIdentity.value(), tenantId.value());

        Map<String, Object> payload = Map.of(
                "requestId", 0,
                "reportBase", "FullInventory"
        );

        commandSender.sendCommand(tenantId, stationIdentity, "GetBaseReport", payload)
                .thenAccept(response -> {
                    String status = response.get("status") != null ? response.get("status").toString() : "unknown";
                    log.info("GetBaseReport response from {}: {}", stationIdentity.value(), status);
                })
                .exceptionally(error -> {
                    log.warn("GetBaseReport failed for {}: {}", stationIdentity.value(), error.getMessage());
                    return null;
                });
    }
}
