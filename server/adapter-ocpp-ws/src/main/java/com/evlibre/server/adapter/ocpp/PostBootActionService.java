package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.core.domain.v16.model.StationConfigurationKey;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.outbound.Ocpp16StationCommandSender;
import com.evlibre.server.core.domain.v16.ports.outbound.StationConfigurationPort;
import com.evlibre.server.core.domain.v201.devicemodel.ReportBase;
import com.evlibre.server.core.domain.v201.ports.inbound.GetBaseReportPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sends post-boot CSMS-initiated messages after a station's BootNotification is accepted.
 * <p>
 * OCPP 1.6: sends GetConfiguration (empty) to retrieve all configuration keys.
 * OCPP 2.0.1: invokes {@link GetBaseReportPort} with {@link ReportBase#FULL_INVENTORY}.
 */
public class PostBootActionService {

    private static final Logger log = LoggerFactory.getLogger(PostBootActionService.class);

    private final Ocpp16StationCommandSender commandSender16;
    private final GetBaseReportPort getBaseReportPort;
    private final StationConfigurationPort configurationPort;
    // Monotonic per-process requestId for post-boot GetBaseReport. B07.FR.04 only
    // requires the station to echo the same id on its follow-up NotifyReport frames;
    // uniqueness across simultaneous station boots lets log entries line up even
    // though inbound aggregation is not wired yet.
    private final AtomicInteger postBootRequestId = new AtomicInteger(1);

    public PostBootActionService(Ocpp16StationCommandSender commandSender16,
                                 GetBaseReportPort getBaseReportPort,
                                 StationConfigurationPort configurationPort) {
        this.commandSender16 = commandSender16;
        this.getBaseReportPort = getBaseReportPort;
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

        commandSender16.sendCommand(tenantId, stationIdentity, "GetConfiguration", Collections.emptyMap())
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
        int requestId = postBootRequestId.getAndIncrement();
        getBaseReportPort.getBaseReport(tenantId, stationIdentity, requestId, ReportBase.FULL_INVENTORY)
                .exceptionally(error -> {
                    log.warn("GetBaseReport failed for {}: {}", stationIdentity.value(), error.getMessage());
                    return null;
                });
    }
}
