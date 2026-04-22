package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ReportBase;
import com.evlibre.server.core.domain.v201.ports.inbound.GetBaseReportPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GetBaseReportUseCaseV201 implements GetBaseReportPort {

    private static final Logger log = LoggerFactory.getLogger(GetBaseReportUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public GetBaseReportUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> getBaseReport(TenantId tenantId,
                                                           ChargePointIdentity stationIdentity,
                                                           int requestId,
                                                           ReportBase reportBase) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(reportBase);

        String reportBaseWire = toWire(reportBase);
        log.info("Sending GetBaseReport({}, requestId={}) to {} (tenant: {})",
                reportBaseWire, requestId, stationIdentity.value(), tenantId.value());

        Map<String, Object> payload = Map.of(
                "requestId", requestId,
                "reportBase", reportBaseWire
        );
        return commandSender.sendCommand(tenantId, stationIdentity, "GetBaseReport", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("GetBaseReport response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }

    private static String toWire(ReportBase reportBase) {
        return switch (reportBase) {
            case CONFIGURATION_INVENTORY -> "ConfigurationInventory";
            case FULL_INVENTORY -> "FullInventory";
            case SUMMARY_INVENTORY -> "SummaryInventory";
        };
    }
}
