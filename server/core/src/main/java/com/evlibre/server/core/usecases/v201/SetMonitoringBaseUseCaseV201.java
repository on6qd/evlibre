package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.MonitoringBase;
import com.evlibre.server.core.domain.v201.dto.GenericDeviceModelStatus;
import com.evlibre.server.core.domain.v201.dto.SetMonitoringBaseResult;
import com.evlibre.server.core.domain.v201.ports.inbound.SetMonitoringBasePort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SetMonitoringBaseUseCaseV201 implements SetMonitoringBasePort {

    private static final Logger log = LoggerFactory.getLogger(SetMonitoringBaseUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public SetMonitoringBaseUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<SetMonitoringBaseResult> setMonitoringBase(TenantId tenantId,
                                                                          ChargePointIdentity stationIdentity,
                                                                          MonitoringBase monitoringBase) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(monitoringBase, "monitoringBase must not be null");

        String wireBase = toWire(monitoringBase);
        log.info("Sending SetMonitoringBase({}) to {} (tenant: {})",
                wireBase, stationIdentity.value(), tenantId.value());

        Map<String, Object> payload = Map.of("monitoringBase", wireBase);

        return commandSender.sendCommand(tenantId, stationIdentity, "SetMonitoringBase", payload)
                .thenApply(SetMonitoringBaseUseCaseV201::decodeResponse);
    }

    private static SetMonitoringBaseResult decodeResponse(Map<String, Object> response) {
        GenericDeviceModelStatus status = statusFromWire((String) response.get("status"));
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> siMap) {
            Object reason = siMap.get("reasonCode");
            if (reason instanceof String s) {
                statusInfoReason = s;
            }
        }
        return new SetMonitoringBaseResult(status, statusInfoReason);
    }

    private static String toWire(MonitoringBase base) {
        return switch (base) {
            case ALL -> "All";
            case FACTORY_DEFAULT -> "FactoryDefault";
            case HARD_WIRED_ONLY -> "HardWiredOnly";
        };
    }

    private static GenericDeviceModelStatus statusFromWire(String wire) {
        return switch (wire) {
            case "Accepted" -> GenericDeviceModelStatus.ACCEPTED;
            case "Rejected" -> GenericDeviceModelStatus.REJECTED;
            case "NotSupported" -> GenericDeviceModelStatus.NOT_SUPPORTED;
            case "EmptyResultSet" -> GenericDeviceModelStatus.EMPTY_RESULT_SET;
            default -> throw new IllegalArgumentException(
                    "Unknown GenericDeviceModelStatus wire value: " + wire);
        };
    }
}
